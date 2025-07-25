package ru.orangesoftware.financisto.activity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.*;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import io.reactivex.disposables.CompositeDisposable;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.datetime.DateUtils;
import ru.orangesoftware.financisto.db.DatabaseHelper.AccountColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.TransactionColumns;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.utils.EnumUtils;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.TransactionUtils;
import ru.orangesoftware.financisto.view.AttributeView;
import ru.orangesoftware.financisto.widget.RateLayoutView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static ru.orangesoftware.financisto.activity.UiUtils.applyTheme;
import static ru.orangesoftware.financisto.model.Category.NO_CATEGORY_ID;
import static ru.orangesoftware.financisto.model.Project.NO_PROJECT_ID;
import static ru.orangesoftware.financisto.utils.Utils.text;

public abstract class AbstractTransactionActivity extends AbstractActivity implements CategorySelector.CategorySelectorListener {

    public static final String TRAN_ID_EXTRA = "tranId";
    public static final String ACCOUNT_ID_EXTRA = "accountId";
    public static final String DUPLICATE_EXTRA = "isDuplicate";
    public static final String TEMPLATE_EXTRA = "isTemplate";
    public static final String DATETIME_EXTRA = "dateTimeExtra";
    public static final String NEW_FROM_TEMPLATE_EXTRA = "newFromTemplateExtra";

    private static final TransactionStatus[] statuses = TransactionStatus.values();

    protected RateLayoutView rateView;

    protected EditText templateName;
    protected TextView accountText;
    protected Cursor accountCursor;
    protected ListAdapter accountAdapter;

    protected Calendar dateTime;
    protected ImageButton status;
    protected Button dateText;
    protected Button timeText;

    protected EditText noteText;
    protected TextView recurText;
    protected TextView notificationText;

    protected Account selectedAccount;

    protected String recurrence;
    protected String notificationOptions;

    protected boolean isDuplicate = false;
    protected boolean isShowPayee = true;

    protected PayeeSelector<AbstractTransactionActivity> payeeSelector;
    protected ProjectSelector<AbstractTransactionActivity> projectSelector;
    protected CategorySelector<AbstractTransactionActivity> categorySelector;

    protected boolean isRememberLastAccount;
    protected boolean isRememberLastCategory;
    protected boolean isRememberLastProject;
    protected boolean isShowNote;
    protected boolean isOpenCalculatorForTemplates;

    protected AttributeView deleteAfterExpired;

    protected DateFormat df;
    protected DateFormat tf;

    protected Transaction transaction = new Transaction();

    protected CompositeDisposable disposable = new CompositeDisposable();

    public AbstractTransactionActivity() {
    }

    protected abstract int getLayoutId();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        df = DateUtils.getLongDateFormat(this);
        tf = DateUtils.getTimeFormat(this);

        long t0 = System.currentTimeMillis();

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(getLayoutId());

        isRememberLastAccount = MyPreferences.isRememberAccount(this);
        isRememberLastCategory = isRememberLastAccount && MyPreferences.isRememberCategory(this);
        isRememberLastProject = isRememberLastCategory && MyPreferences.isRememberProject(this);
        isShowNote = MyPreferences.isShowNote(this);
        isOpenCalculatorForTemplates = MyPreferences.isOpenCalculatorForTemplates(this);

        categorySelector = new CategorySelector<>(this, db, x);
        categorySelector.setListener(this);
        fetchCategories();

        long accountId = -1;
        long transactionId = -1;
        boolean isNewFromTemplate = false;
        final Intent intent = getIntent();
        if (intent != null) {
            accountId = intent.getLongExtra(ACCOUNT_ID_EXTRA, -1);
            transactionId = intent.getLongExtra(TRAN_ID_EXTRA, -1);
            transaction.dateTime = intent.getLongExtra(DATETIME_EXTRA, System.currentTimeMillis());
            if (transactionId != -1) {
                transaction = db.getTransaction(transactionId);
                transaction.categoryAttributes = db.getAllAttributesForTransaction(transactionId);
                isDuplicate = intent.getBooleanExtra(DUPLICATE_EXTRA, false);
                isNewFromTemplate = intent.getBooleanExtra(NEW_FROM_TEMPLATE_EXTRA, false);
                if (isDuplicate) {
                    transaction.id = -1;
                    transaction.dateTime = System.currentTimeMillis();
                }
            }
            transaction.isTemplate = intent.getIntExtra(TEMPLATE_EXTRA, transaction.isTemplate);
        }

        if (transaction.id == -1) {
            accountCursor = db.getAllActiveAccounts();
        } else {
            accountCursor = db.getAccountsForTransaction(transaction);
        }
        startManagingCursor(accountCursor);
        accountAdapter = TransactionUtils.createAccountAdapter(this, accountCursor);

        dateTime = Calendar.getInstance();
        Date date = dateTime.getTime();

        status = findViewById(R.id.status);
        status.setOnClickListener(v -> {
            ArrayAdapter<String> adapter = EnumUtils.createDropDownAdapter(AbstractTransactionActivity.this, statuses);
            x.selectPosition(AbstractTransactionActivity.this, R.id.status, R.string.transaction_status, adapter, transaction.status.ordinal());
        });

        dateText = findViewById(R.id.date);
        dateText.setText(df.format(date));
        dateText.setOnClickListener(arg0 -> {
            DatePickerDialog dialog = DatePickerDialog.newInstance(
                    (view, year, monthOfYear, dayOfMonth) -> {
                        dateTime.set(year, monthOfYear, dayOfMonth);
                        dateText.setText(df.format(dateTime.getTime()));
                    },
                    dateTime.get(Calendar.YEAR),
                    dateTime.get(Calendar.MONTH),
                    dateTime.get(Calendar.DAY_OF_MONTH)
            );
            applyTheme(this, dialog);
            dialog.show(getFragmentManager(), "DatePickerDialog");
        });

        timeText = findViewById(R.id.time);
        timeText.setText(tf.format(date));
        timeText.setOnClickListener(arg0 -> {
            boolean is24Format = DateUtils.is24HourFormat(AbstractTransactionActivity.this);
            TimePickerDialog dialog = TimePickerDialog.newInstance(
                    (view, hourOfDay, minute, second) -> {
                        dateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        dateTime.set(Calendar.MINUTE, minute);
                        timeText.setText(tf.format(dateTime.getTime()));
                    },
                    dateTime.get(Calendar.HOUR_OF_DAY), dateTime.get(Calendar.MINUTE), is24Format
            );
            applyTheme(this, dialog);
            dialog.show(getFragmentManager(), "TimePickerDialog");
        });

        internalOnCreate();

        LinearLayout layout = findViewById(R.id.list);

        this.templateName = new EditText(this);
        if (transaction.isTemplate()) {
            x.addEditNode(layout, R.string.template_name, templateName);
        }

        rateView = new RateLayoutView(this, x, layout);


        projectSelector = new ProjectSelector<>(this, db, x);
        projectSelector.fetchEntities();

        createListNodes(layout);
        categorySelector.createAttributesLayout(layout);
        createCommonNodes(layout);

        Button bSave = findViewById(R.id.bSave);
        bSave.setOnClickListener(arg0 -> saveAndFinish());

        final boolean isEdit = transaction.id > 0;
        Button bSaveAndNew = findViewById(R.id.bSaveAndNew);
        if (isEdit) {
            bSaveAndNew.setText(R.string.cancel);
        }
        bSaveAndNew.setOnClickListener(arg0 -> {
            if (isEdit) {
                setResult(RESULT_CANCELED);
                finish();
            } else {
                if (saveAndFinish()) {
                    intent.putExtra(DATETIME_EXTRA, transaction.dateTime);
                    startActivityForResult(intent, -1);
                }
            }
        });

        if (transactionId != -1) {
            isOpenCalculatorForTemplates &= isNewFromTemplate;
            editTransaction(transaction);
        } else {
            setDateTime(transaction.dateTime);
            categorySelector.selectCategory(NO_CATEGORY_ID);
            if (accountId != -1) {
                selectAccount(accountId);
            } else {
                long lastAccountId = MyPreferences.getLastAccount(this);
                if (isRememberLastAccount && lastAccountId != -1) {
                    selectAccount(lastAccountId);
                }
            }
            if (!isRememberLastProject) {
                projectSelector.selectEntity(NO_PROJECT_ID);
            }
        }

        long t1 = System.currentTimeMillis();
        Log.i("TransactionActivity", "onCreate " + (t1 - t0) + "ms");
    }

    protected void createPayeeNode(LinearLayout layout) {
        payeeSelector = new PayeeSelector<>(this, db, x);
        payeeSelector.fetchEntities();
        payeeSelector.createNode(layout);
    }

    protected abstract void fetchCategories();

    private boolean saveAndFinish() {
        long id = save();
        if (id > 0) {
            Intent data = new Intent();
            data.putExtra(TransactionColumns._id.name(), id);
            setResult(RESULT_OK, data);
            finish();
            return true;
        }
        return false;
    }

    private long save() {
        if (onOKClicked()) {
            boolean isNew = transaction.id == -1;
            long id = db.insertOrUpdate(transaction, getAttributes());
            if (isNew) {
                MyPreferences.setLastAccount(this, transaction.fromAccountId);
            }
            return id;
        }
        return -1;
    }

    private List<TransactionAttribute> getAttributes() {
        List<TransactionAttribute> attributes = categorySelector.getAttributes();
        if (deleteAfterExpired != null) {
            TransactionAttribute ta = deleteAfterExpired.newTransactionAttribute();
            attributes.add(ta);
        }
        return attributes;
    }

    protected abstract void internalOnCreate();

    protected void createCommonNodes(LinearLayout layout) {
        int noteOrder = MyPreferences.getNoteOrder(this);
        int projectOrder = MyPreferences.getProjectOrder(this);
        for (int i = 0; i < 6; i++) {
            if (i == noteOrder) {
                if (isShowNote) {
                    //note
                    noteText = new EditText(this);
                    noteText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                    x.addEditNode(layout, R.string.note, noteText);
                }
            }
            if (i == projectOrder) {
                projectSelector.createNode(layout);
            }
        }
    }

    protected abstract void createListNodes(LinearLayout layout);

    protected abstract boolean onOKClicked();

    @Override
    protected void onClick(View v, int id) {
        if (isShowPayee) payeeSelector.onClick(id);
        projectSelector.onClick(id);
        categorySelector.onClick(id);
        switch (id) {
            case R.id.account:
                x.select(this, R.id.account, R.string.account, accountCursor, accountAdapter,
                        AccountColumns.ID, getSelectedAccountId());
                break;
        }
    }

    @Override
    public void onSelectedPos(int id, int selectedPos) {
        if (isShowPayee) payeeSelector.onSelectedPos(id, selectedPos);
        projectSelector.onSelectedPos(id, selectedPos);
        switch (id) {
            case R.id.status:
                selectStatus(statuses[selectedPos]);
                break;
        }
    }

    @Override
    public void onSelectedId(int id, long selectedId) {
        if (isShowPayee) payeeSelector.onSelectedId(id, selectedId);
        categorySelector.onSelectedId(id, selectedId);
        projectSelector.onSelectedId(id, selectedId);
        switch (id) {
            case R.id.account:
                selectAccount(selectedId);
                break;
        }
    }

    private void selectStatus(TransactionStatus transactionStatus) {
        transaction.status = transactionStatus;
        status.setImageResource(transactionStatus.iconId);
    }

    protected Account selectAccount(long accountId) {
        return selectAccount(accountId, true);
    }

    protected Account selectAccount(long accountId, boolean selectLast) {
        Account a = db.getAccount(accountId);
        if (a != null) {
            accountText.setText(a.title);
            rateView.selectCurrencyFrom(a.currency);
            selectedAccount = a;
        }
        return a;
    }

    protected long getSelectedAccountId() {
        return selectedAccount != null ? selectedAccount.id : -1;
    }

    @Override
    public void onCategorySelected(Category category, boolean selectLast) {
        addOrRemoveSplits();
        categorySelector.addAttributes(transaction);
        switchIncomeExpenseButton(category);
        if (selectLast && isRememberLastProject) {
            projectSelector.selectEntity(category.lastProjectId);
        }
        projectSelector.setNodeVisible(!category.isSplit());
    }

    protected void addOrRemoveSplits() {
    }

    protected void switchIncomeExpenseButton(Category category) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        projectSelector.onActivityResult(requestCode, resultCode, data);
        categorySelector.onActivityResult(requestCode, resultCode, data);
        if (isShowPayee) {
            payeeSelector.onActivityResult(requestCode, resultCode, data);
        }
    }


    protected void setDateTime(long date) {
        Date d = new Date(date);
        dateTime.setTime(d);
        dateText.setText(df.format(d));
        timeText.setText(tf.format(d));
    }

    protected abstract void editTransaction(Transaction transaction);

    protected void commonEditTransaction(Transaction transaction) {
        selectStatus(transaction.status);
        categorySelector.selectCategory(transaction.categoryId, false);
        projectSelector.selectEntity(transaction.projectId);
        setDateTime(transaction.dateTime);
        if (isShowNote) {
            noteText.setText(transaction.note);
        }
        if (transaction.isTemplate()) {
            templateName.setText(transaction.templateName);
        }

        if (transaction.isCreatedFromTemlate() && isOpenCalculatorForTemplates) {
            rateView.openFromAmountCalculator();
        }
    }

    protected boolean checkSelectedEntities() {
        if (isShowPayee) {
            payeeSelector.createNewEntity();
        }
        projectSelector.createNewEntity();
        return true;
    }

    protected void updateTransactionFromUI(Transaction transaction) {
        transaction.categoryId = categorySelector.getSelectedCategoryId();
        transaction.projectId = projectSelector.getSelectedEntityId();
        transaction.dateTime = dateTime.getTime().getTime();
        if (isShowPayee) {
            transaction.payeeId = payeeSelector.getSelectedEntityId();
        }
        if (isShowNote) {
            transaction.note = text(noteText);
        }
        if (transaction.isTemplate()) {
            transaction.templateName = text(templateName);
        }
    }

    protected void selectPayee(long payeeId) {
        if (isShowPayee) {
            payeeSelector.selectEntity(payeeId);
        }
    }

    @Override
    protected void onDestroy() {
        disposable.dispose();
        if (categorySelector != null) categorySelector.onDestroy();
        super.onDestroy();
    }
}
