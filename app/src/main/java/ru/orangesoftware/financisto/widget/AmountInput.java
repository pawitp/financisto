/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.databinding.AmountInputBinding;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.Utils;

public class AmountInput extends LinearLayout implements AmountListener {

    public interface OnAmountChangedListener {
        void onAmountChanged(long oldAmount, long newAmount);
    }

    private static final AtomicInteger EDIT_AMOUNT_REQUEST = new AtomicInteger(2000);

    protected Activity owner;
    private Currency currency;

    protected ImageSwitcher signSwitcher;
    protected EditText primary;
    protected TextView delimiter;
    protected EditText secondary;

    protected int minHeight;

    protected Drawable plusDrawable;
    protected int plusColor;
    protected Drawable minusDrawable;
    protected int minusColor;

    private int requestId;
    private OnAmountChangedListener onAmountChangedListener;
    private boolean incomeExpenseEnabled = true;
    private boolean isExpense = true;

    protected AmountInput(Context context) {
        super(context);
        initResources();
    }

    public void disableIncomeExpenseButton() {
        incomeExpenseEnabled = false;
        signSwitcher.setEnabled(false);
    }

    public boolean isIncomeExpenseEnabled() {
        return incomeExpenseEnabled;
    }

    public void setIncome() {
        if (isExpense) {
            onClickSignSwitcher();
        }
    }

    public void setExpense() {
        if (!isExpense) {
            onClickSignSwitcher();
        }
    }

    public boolean isExpense() {
        return isExpense;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        Utils.setEnabled(this, enabled);
        if (!incomeExpenseEnabled) {
            disableIncomeExpenseButton();
        }
    }

    public void setOnAmountChangedListener(
            OnAmountChangedListener onAmountChangedListener) {
        this.onAmountChangedListener = onAmountChangedListener;
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        private long oldAmount;

        @Override
        public void afterTextChanged(Editable s) {
            if (onAmountChangedListener != null) {
                long amount = getAmount();
                onAmountChangedListener.onAmountChanged(oldAmount, amount);
                oldAmount = amount;
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
            oldAmount = getAmount();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
        }
    };

    protected void initialize() {
        setMinimumHeight(minHeight);
        plusDrawable.mutate().setColorFilter(plusColor, PorterDuff.Mode.SRC_ATOP);
        minusDrawable.mutate().setColorFilter(minusColor, PorterDuff.Mode.SRC_ATOP);
        requestId = EDIT_AMOUNT_REQUEST.incrementAndGet();
        signSwitcher.setFactory(() -> {
            ImageView v = new ImageView(getContext());
            v.setScaleType(ImageView.ScaleType.FIT_CENTER);
            v.setLayoutParams(new ImageSwitcher.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            return v;
        });
        signSwitcher.setImageDrawable(minusDrawable);
        primary.setKeyListener(keyListener);
        primary.addTextChangedListener(textWatcher);
        primary.setOnFocusChangeListener(selectAllOnFocusListener);
        secondary.setKeyListener(new DigitsKeyListener(false, false) {

            @Override
            public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    if (content.length() == 0) {
                        primary.requestFocus();
                        int pos = primary.getText().length();
                        primary.setSelection(pos, pos);
                        return true;
                    }
                }
                return super.onKeyDown(view, content, keyCode, event);
            }

            @Override
            public int getInputType() {
                return InputType.TYPE_CLASS_PHONE;
            }

        });
        secondary.addTextChangedListener(textWatcher);
        secondary.setOnFocusChangeListener(selectAllOnFocusListener);

        if (!MyPreferences.isEnterCurrencyDecimalPlaces(getContext())) {
            secondary.setVisibility(GONE);
            delimiter.setVisibility(GONE);
        }
    }

    protected void onClickCalculator() {
        openCalculator();
    }

    protected void onClickSignSwitcher() {
        if (isExpense) {
            isExpense = false;
            signSwitcher.setImageDrawable(plusDrawable);
            notifyAmountChangedListener();
        } else {
            isExpense = true;
            signSwitcher.setImageDrawable(minusDrawable);
            notifyAmountChangedListener();
        }
    }

    private void notifyAmountChangedListener() {
        if (onAmountChangedListener != null) {
            long amount = getAmount();
            onAmountChangedListener.onAmountChanged(-amount, amount);
        }
    }

    private static final char[] acceptedChars = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    private static final char[] commaChars = new char[]{'.', ','};

    private final NumberKeyListener keyListener = new NumberKeyListener() {

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (end - start == 1) {
                char c = source.charAt(0);
                if (c == '.' || c == ',') {
                    onDotOrComma();
                    return "";
                }
                if (isIncomeExpenseEnabled()) {
                    if (c == '-') {
                        setExpense();
                        return "";
                    }
                    if (c == '+') {
                        setIncome();
                        return "";
                    }
                }
            }
            return super.filter(source, start, end, dest, dstart, dend);
        }

        @Override
        public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event) {
            char c = event.getMatch(commaChars);
            if (c == '.' || c == ',') {
                onDotOrComma();
                return true;
            }
            return super.onKeyDown(view, content, keyCode, event);
        }

        @Override
        protected char[] getAcceptedChars() {
            return acceptedChars;
        }

        @Override
        public int getInputType() {
            return InputType.TYPE_CLASS_PHONE;
        }
    };

    private final View.OnFocusChangeListener selectAllOnFocusListener = (v, hasFocus) -> {
        EditText t = (EditText) v;
        if (hasFocus) {
            t.selectAll();
        }
    };

    protected void onDotOrComma() {
        secondary.requestFocus();
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public void setOwner(Activity owner) {
        this.owner = owner;
    }

    public void setAmount(long amount) {
        long absAmount = Math.abs(amount);
        long x = absAmount / 100;
        primary.setText(String.valueOf(x));

        if (MyPreferences.isEnterCurrencyDecimalPlaces(getContext())) {
            long y = absAmount - 100 * x;
            secondary.setText(String.format("%02d", y));
        }

        if (isIncomeExpenseEnabled() && amount != 0) {
            if (amount > 0) {
                setIncome();
            } else {
                setExpense();
            }
        }
    }

    public long getAmount() {
        String p = primary.getText().toString();
        String s = secondary.getText().toString();
        long x = 100 * toLong(p);
        long y = toLong(s);
        long amount = x + (s.length() == 1 ? 10 * y : y);
        return isExpense() ? -amount : amount;
    }

    private String getAbsAmountString() {
        String p = primary.getText().toString().trim();
        String s = secondary.getText().toString().trim();
        return (Utils.isNotEmpty(p) ? p : "0") + "."
                + (Utils.isNotEmpty(s) ? s : "0");
    }

    private long toLong(String s) {
        try {
            return s == null || s.length() == 0 ? 0 : Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void setColor(int color) {
        primary.setTextColor(color);
        secondary.setTextColor(color);
    }

    public void openCalculator() {
        CalculatorInput input = CalculatorInput.newInstance(getAbsAmountString());
        input.setListener(this);
        input.show(owner.getFragmentManager(), "calculator");
    }

    @Override
    public void onAmountChanged(String amount) {
        try {
            long oldAmount = getAmount();
            BigDecimal d = new BigDecimal(amount).setScale(2, BigDecimal.ROUND_HALF_UP);
            boolean wasExpense = isExpense();
            setAmount(d.unscaledValue().longValue());
            if (wasExpense) setExpense();
            if (onAmountChangedListener != null) {
                onAmountChangedListener.onAmountChanged(oldAmount, getAmount());
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void initResources() {
        Resources resources = getContext().getResources();
        this.minHeight = resources.getDimensionPixelSize(R.dimen.select_entry_height_no_label);
        this.plusDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_action_add);
        this.minusDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_action_minus);
        this.plusColor = ContextCompat.getColor(getContext(), R.color.positive_amount);
        this.minusColor = ContextCompat.getColor(getContext(), R.color.negative_amount);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        inflate(getContext(), R.layout.amount_input, this);
        initViews();
    }

    private void initViews() {
        AmountInputBinding binding = AmountInputBinding.bind(this);;
        this.signSwitcher = binding.signSwitcher;
        this.primary = binding.primary;
        this.delimiter = binding.delimiter;
        this.secondary = binding.secondary;

        binding.calculator.setOnClickListener(v -> onClickCalculator());
        binding.signSwitcher.setOnClickListener(v -> onClickSignSwitcher());
        initialize();
    }

    public static AmountInput build(Context context) {
        AmountInput instance = new AmountInput(context);
        instance.onFinishInflate();
        return instance;
    }

}
