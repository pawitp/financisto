package ru.orangesoftware.financisto.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ScrollView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseHelper.AttributeColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.CategoryColumns;
import ru.orangesoftware.financisto.model.Attribute;
import ru.orangesoftware.financisto.model.Category;

import static ru.orangesoftware.financisto.activity.CategorySelector.SelectorType.PARENT;
import static ru.orangesoftware.financisto.utils.Utils.checkEditText;
import static ru.orangesoftware.financisto.utils.Utils.text;

public class CategoryActivity extends AbstractActivity implements CategorySelector.CategorySelectorListener {

    public static final String CATEGORY_ID_EXTRA = "categoryId";
    public static final int NEW_ATTRIBUTE_REQUEST = 1;
    public static final int EDIT_ATTRIBUTE_REQUEST = 2;

    private String[] types;

    private Cursor attributeCursor;
    private ListAdapter attributeAdapter;

    private ToggleButton incomeExpenseButton;

    private EditText categoryTitle;

    private ScrollView scrollView;
    private LinearLayout attributesLayout;
    private LinearLayout parentAttributesLayout;

    private Category category = new Category(-1);

    private CategorySelector parentCatSelector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.category);

        types = getResources().getStringArray(R.array.attribute_types);

        scrollView = findViewById(R.id.scroll);

        categoryTitle = new EditText(this);
        categoryTitle.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        categoryTitle.setSingleLine();

        Intent intent = getIntent();
        if (intent != null) {
            long id = intent.getLongExtra(CATEGORY_ID_EXTRA, -1);
            if (id != -1) {
                category = db.getCategoryWithParent(id);
            }
        }

        attributeCursor = db.getAllAttributes();
        startManagingCursor(attributeCursor);
        attributeAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_dropdown_item,
                attributeCursor, new String[]{AttributeColumns.TITLE}, new int[]{android.R.id.text1});

        parentCatSelector = initParentCategorySelector();

        LinearLayout titleLayout = new LinearLayout(this);
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        layoutInflater.inflate(R.layout.category_title, titleLayout, true);
        incomeExpenseButton = titleLayout.findViewById(R.id.toggle);
        categoryTitle = titleLayout.findViewById(R.id.primary);
        LinearLayout layout = findViewById(R.id.layout);
        x.addEditNode(layout, R.string.title, titleLayout);

        attributesLayout = x.addTitleNodeNoDivider(layout, R.string.attributes).findViewById(R.id.layout);
        x.addInfoNodePlus(attributesLayout, R.id.new_attribute, R.id.add_attribute, R.string.add_attribute);
        addAttributes();
        parentAttributesLayout = x.addTitleNodeNoDivider(layout, R.string.parent_attributes).findViewById(R.id.layout);
        addParentAttributes();

        Button bOk = findViewById(R.id.bOK);
        bOk.setOnClickListener(view -> {
            if (checkEditText(categoryTitle, "title", true, 100)) {
                category.title = text(categoryTitle);
                setCategoryType(category);
                int count = attributesLayout.getChildCount();
                ArrayList<Attribute> attributes = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    View v = attributesLayout.getChildAt(i);
                    Object o = v.getTag();
                    if (o instanceof Attribute) {
                        attributes.add((Attribute) o);
                    }
                }
                long id = db.insertOrUpdate(category, attributes);
                Intent data = new Intent();
                data.putExtra(CategoryColumns._id.name(), id);
                setResult(RESULT_OK, data);
                finish();
            }
        });

        Button bCancel = findViewById(R.id.bCancel);
        bCancel.setOnClickListener(view -> {
            setResult(RESULT_CANCELED, null);
            finish();
        });

        editCategory();
    }

    private CategorySelector initParentCategorySelector() {
        final CategorySelector res = new CategorySelector<>(this, db, x, category.id);
        LinearLayout layout = findViewById(R.id.layout);
        res.createNode(layout, PARENT);
        res.setListener(this);
        res.fetchCategories(false);
        res.doNotShowSplitCategory();
        return res;
    }

    private void setCategoryType(Category category) {
        if (category.getParentId() > 0) {
            category.copyTypeFromParent();
        } else {
            if (incomeExpenseButton.isChecked()) {
                category.makeThisCategoryIncome();
            } else {
                category.makeThisCategoryExpense();
            }
        }
    }

    private void editCategory() {
        categoryTitle.setText(category.title);
        parentCatSelector.selectCategory(category.getParentId(), false);
    }

    private void updateIncomeExpenseType() {
        if (category.getParentId() > 0) {
            if (category.parent.isIncome()) {
                incomeExpenseButton.setChecked(true);
            } else {
                incomeExpenseButton.setChecked(false);
            }
            incomeExpenseButton.setEnabled(false);
        } else {
            incomeExpenseButton.setChecked(category.isIncome());
            incomeExpenseButton.setEnabled(true);
        }
    }

    private void addAttributes() {
        long categoryId = category.id;
        if (categoryId == -1) {
            categoryId = 0;
        }
        ArrayList<Attribute> attributes = db.getAttributesForCategory(categoryId);
        for (Attribute a : attributes) {
            addAttribute(a);
        }
    }

    private void addParentAttributes() {
        long categoryId = category.getParentId();
        ArrayList<Attribute> attributes = db.getAllAttributesForCategory(categoryId);
        if (attributes.size() > 0) {
            for (Attribute a : attributes) {
                View v = x.inflater.new Builder(parentAttributesLayout, R.layout.select_entry_simple).create();
                v.setTag(a);
                setAttributeData(v, a);
            }
        } else {
            x.addInfoNodeSingle(parentAttributesLayout, -1, R.string.no_attributes);
        }
    }

    private void addAttribute(Attribute a) {
        View v = x.inflater.new Builder(attributesLayout, R.layout.select_entry_simple_minus).withId(R.id.edit_attribute, this).create();
        setAttributeData(v, a);
        ImageView plusImageView = v.findViewById(R.id.plus_minus);
        plusImageView.setId(R.id.remove_attribute);
        plusImageView.setOnClickListener(this);
        plusImageView.setTag(v.getTag());
        v.setTag(a);
        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

    private void setAttributeData(View v, Attribute a) {
        TextView labelView = v.findViewById(R.id.label);
        labelView.setText(a.title);
        TextView dataView = v.findViewById(R.id.data);
        dataView.setText(types[a.type - 1]);
    }

    @Override
    protected void onClick(final View v, final int id) {
        switch (id) {
            case R.id.category:
            case R.id.category_show_filter:
            case R.id.category_close_filter:
            case R.id.category_show_list:
                parentCatSelector.onClick(R.id.category);
                break;

            // Attributes >>
            case R.id.new_attribute:
                x.select(this, R.id.new_attribute, R.string.attribute, attributeCursor, attributeAdapter,
                        AttributeColumns.ID, -1);
                break;
            case R.id.add_attribute: {
                Intent intent = new Intent(this, AttributeActivity.class);
                startActivityForResult(intent, NEW_ATTRIBUTE_REQUEST);
            }
            break;
            case R.id.edit_attribute: {
                Object o = v.getTag();
                if (o instanceof Attribute) {
                    Intent intent = new Intent(this, AttributeActivity.class);
                    intent.putExtra(AttributeColumns.ID, ((Attribute) o).id);
                    startActivityForResult(intent, EDIT_ATTRIBUTE_REQUEST);
                }
            }
            break;
            case R.id.remove_attribute:
                attributesLayout.removeView((View) v.getTag());
                attributesLayout.removeView((View) v.getParent());
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                break;
        }
    }

    @Override
    public void onSelectedId(int id, long selectedId) {
        switch (id) {
            case R.id.category:
                parentCatSelector.selectCategory(selectedId);
                break;
            case R.id.new_attribute:
                Attribute a = db.getAttribute(selectedId);
                addAttribute(a);
                break;
        }
    }

    private void selectParentCategory(Category c) {
        if (c != null) {
            category.parent = c;
        }
        updateIncomeExpenseType();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case NEW_ATTRIBUTE_REQUEST: {
                    long attributeId = data.getLongExtra(AttributeColumns.ID, -1);
                    if (attributeId != -1) {
                        Attribute a = db.getAttribute(attributeId);
                        addAttribute(a);
                    }
                }
                break;
                case EDIT_ATTRIBUTE_REQUEST: {
                    long attributeId = data.getLongExtra(AttributeColumns.ID, -1);
                    if (attributeId != -1) {
                        Attribute a = db.getAttribute(attributeId);
                        attributeCursor.requery();
                        updateAttribute(attributesLayout, a);
                        updateAttribute(parentAttributesLayout, a);
                    }
                }
                break;
                case R.id.category_pick: {
                    parentCatSelector.onActivityResult(requestCode, resultCode, data);
                }
                break;
            }
        }
    }

    @Deprecated
    private void updateAttribute(LinearLayout layout, Attribute a) {
        int count = layout.getChildCount();
        for (int i = 0; i < count; i++) {
            View v = layout.getChildAt(i);
            Object o = v.getTag();
            if (o instanceof Attribute) {
                Attribute a2 = (Attribute) o;
                if (a2.id == a.id) {
                    setAttributeData(v, a);
                }
            }
        }
    }


    @Override
    public void onCategorySelected(Category parent, boolean selectLast) {
        if (parent.id != category.id) {
            selectParentCategory(parent);
        }
    }
}
