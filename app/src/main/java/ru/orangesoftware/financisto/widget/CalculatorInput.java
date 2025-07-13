package ru.orangesoftware.financisto.widget;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import androidx.core.content.ContextCompat;

import android.os.VibratorManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.databinding.CalculatorBinding;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.StringUtil;
import ru.orangesoftware.financisto.utils.Utils;

public class CalculatorInput extends DialogFragment {

    public static final String AMOUNT_ARG = "amount";

    private CalculatorBinding binding;
    protected TextView tvResult;

    protected TextView tvOp;

    protected List<Button> buttons;

    protected Vibrator vibrator;

    protected String amount;

    private final Stack<String> stack = new Stack<>();
    private String result = "0";
    private boolean isRestart = true;
    private boolean isInEquals = false;
    private char lastOp = '\0';
    private AmountListener listener;

    public void setListener(AmountListener listener) {
        this.listener = listener;
    }

    public void init() {
        vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
    }

    private void initViews() {
        tvResult = binding.result;
        tvOp = binding.op;
        
        buttons = new ArrayList<>();
        buttons.add(binding.b0);
        buttons.add(binding.b1);
        buttons.add(binding.b2);
        buttons.add(binding.b3);
        buttons.add(binding.b4);
        buttons.add(binding.b5);
        buttons.add(binding.b6);
        buttons.add(binding.b7);
        buttons.add(binding.b8);
        buttons.add(binding.b9);
        buttons.add(binding.bAdd);
        buttons.add(binding.bSubtract);
        buttons.add(binding.bDivide);
        buttons.add(binding.bMultiply);
        buttons.add(binding.bPercent);
        buttons.add(binding.bPlusMinus);
        buttons.add(binding.bDot);
        buttons.add(binding.bResult);
        buttons.add(binding.bClear);
        buttons.add(binding.bDelete);
        
        for (Button button : buttons) {
            button.setOnClickListener(this::onButtonClick);
        }
        
        View bOK = binding.getRoot().findViewById(R.id.bOK);
        View bCancel = binding.getRoot().findViewById(R.id.bCancel);
        if (bOK != null) {
            bOK.setOnClickListener(v -> onOk());
        }
        if (bCancel != null) {
            bCancel.setOnClickListener(v -> onCancel());
        }
    }

    public void initUi() {
        int bgColorResource = R.color.mdtp_date_picker_view_animator_dark_theme;
        int bgColor = ContextCompat.getColor(getActivity(), bgColorResource);
        getView().setBackgroundColor(bgColor);
        setDisplay(amount);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null && args.containsKey(AMOUNT_ARG)) {
            amount = args.getString(AMOUNT_ARG);
        }
        init();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = CalculatorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews();
        initUi();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        tvResult = null;
        tvOp = null;
        buttons = null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    public void onButtonClick(View v) {
        Button b = (Button) v;
        char c = b.getText().charAt(0);
        onButtonClick(c);
    }

    public void onOk() {
        if (!isInEquals) {
            doEqualsChar();
        }
        listener.onAmountChanged(result);
        dismiss();
    }

    public void onCancel() {
        dismiss();
    }

    private void setDisplay(String s) {
        if (Utils.isNotEmpty(s)) {
            s = s.replaceAll(",", ".");
            result = s;
            tvResult.setText(s);
        }
    }

    private void onButtonClick(char c) {
        if (vibrator != null && MyPreferences.isPinHapticFeedbackEnabled(getActivity())) {
            vibrator.vibrate(20);
        }
        switch (c) {
            case 'C':
                resetAll();
                break;
            case '<':
                doBackspace();
                break;
            default:
                doButton(c);
                break;
        }
    }

    private void resetAll() {
        setDisplay("0");
        tvOp.setText("");
        lastOp = '\0';
        isRestart = true;
        stack.clear();
    }

    private void doBackspace() {
        String s = tvResult.getText().toString();
        if ("0".equals(s) || isRestart) {
            return;
        }
        String newDisplay = s.length() > 1 ? s.substring(0, s.length() - 1) : "0";
        if ("-".equals(newDisplay)) {
            newDisplay = "0";
        }
        setDisplay(newDisplay);
    }

    private void doButton(char c) {
        if (Character.isDigit(c) || c == '.') {
            addChar(c);
        } else {
            switch (c) {
                case '+':
                case '-':
                case '/':
                case '*':
                    doOpChar(c);
                    break;
                case '%':
                    doPercentChar();
                    break;
                case '=':
                case '\r':
                    doEqualsChar();
                    break;
                case '\u00B1':
                    setDisplay(new BigDecimal(result).negate().toPlainString());
                    break;
            }
        }
    }

    private void addChar(char c) {
        String s = tvResult.getText().toString();
        if (c == '.' && s.indexOf('.') != -1 && !isRestart) {
            return;
        }
        if ("0".equals(s)) {
            s = String.valueOf(c);
        } else {
            s += c;
        }
        setDisplay(s);
        if (isRestart) {
            setDisplay(String.valueOf(c));
            isRestart = false;
        }
    }

    private void doOpChar(char op) {
        if (isInEquals) {
            stack.clear();
            isInEquals = false;
        }
        stack.push(result);
        doLastOp();
        lastOp = op;
        tvOp.setText(String.valueOf(lastOp));
    }

    private void doLastOp() {
        isRestart = true;
        if (lastOp == '\0' || stack.size() == 1) {
            return;
        }

        String valTwo = stack.pop();
        String valOne = stack.pop();
        switch (lastOp) {
            case '+':
                stack.push(asNumber(valOne).add(asNumber(valTwo)).toPlainString());
                break;
            case '-':
                stack.push(asNumber(valOne).subtract(asNumber(valTwo)).toPlainString());
                break;
            case '*':
                stack.push(asNumber(valOne).multiply(asNumber(valTwo)).toPlainString());
                break;
            case '/':
                BigDecimal d2 = asNumber(valTwo);
                if (d2.intValue() == 0) {
                    stack.push("0.0");
                } else {
                    stack.push(asNumber(valOne).divide(d2, 2, BigDecimal.ROUND_HALF_UP).toPlainString());
                }
                break;
            default:
                break;
        }
        setDisplay(stack.peek());
        if (isInEquals) {
            stack.push(valTwo);
        }
    }

    private BigDecimal asNumber(String s) {
        if (StringUtil.isEmpty(s)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(s);
    }

    private void doPercentChar() {
        if (stack.size() == 0)
            return;
        setDisplay(new BigDecimal(result).divide(Utils.HUNDRED, 2, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(stack.peek())).toPlainString());
        tvOp.setText("");
    }

    private void doEqualsChar() {
        if (lastOp == '\0') {
            return;
        }
        if (!isInEquals) {
            isInEquals = true;
            stack.push(result);
        }
        doLastOp();
        tvOp.setText("");
    }

    public static CalculatorInput newInstance(String amount) {
        CalculatorInput fragment = new CalculatorInput();
        Bundle args = new Bundle();
        if (amount != null) {
            args.putString(AMOUNT_ARG, amount);
        }
        fragment.setArguments(args);
        return fragment;
    }

}
