package ru.orangesoftware.financisto.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.RelativeLayout.LayoutParams;
import ru.orangesoftware.financisto.R;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class NodeInflater {

    private final LayoutInflater inflater;

    public NodeInflater(LayoutInflater inflater) {
        this.inflater = inflater;
    }

    public View addDivider(LinearLayout layout) {
        View divider = inflater.inflate(R.layout.edit_divider, layout, false);
        layout.addView(divider);
        return divider;
    }

    public class Builder {
        protected final LinearLayout layout;
        protected final View v;

        private boolean divider = true;

        public Builder(LinearLayout layout, int layoutId) {
            this.layout = layout;
            this.v = inflater.inflate(layoutId, layout, false);
        }

        public Builder(LinearLayout layout, View v) {
            this.layout = layout;
            this.v = v;
        }

        public Builder withId(int id, OnClickListener listener) {
            v.setId(id);
            v.setOnClickListener(listener);
            return this;
        }

        public Builder withLabel(int labelId) {
            TextView labelView = v.findViewById(R.id.label);
            labelView.setText(labelId);
            return this;
        }

        public Builder withLabel(String label) {
            TextView labelView = v.findViewById(R.id.label);
            labelView.setText(label);
            return this;
        }

        public Builder withData(int labelId) {
            TextView labelView = v.findViewById(R.id.data);
            labelView.setText(labelId);
            return this;
        }

        public Builder withData(String label) {
            TextView labelView = v.findViewById(R.id.data);
            labelView.setText(label);
            return this;
        }

        public Builder withIcon(int iconId) {
            ImageView iconView = v.findViewById(R.id.icon);
            iconView.setImageResource(iconId);
            return this;
        }

        public Builder withNoDivider() {
            divider = false;
            return this;
        }

        public View create() {
            layout.addView(v);
            if (divider) {
                View dividerView = addDivider(layout);
                v.setTag(dividerView);
            }
            return v;
        }

    }

    public class EditBuilder extends Builder {

        public EditBuilder(LinearLayout layout, View view) {
            super(layout, R.layout.select_entry_edit);
            RelativeLayout relativeLayout = v.findViewById(R.id.layout);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.ALIGN_LEFT, R.id.label);
            layoutParams.addRule(RelativeLayout.BELOW, R.id.label);
            relativeLayout.addView(view, layoutParams);
        }

    }

    public class ListBuilder extends Builder {

        public ListBuilder(LinearLayout layout, int layoutId) {
            super(layout, layoutId);
        }

        public ListBuilder withButtonId(int buttonId, OnClickListener listener) {
            ImageView plusImageView = v.findViewById(R.id.plus_minus);
            if (buttonId > 0) {
                plusImageView.setVisibility(VISIBLE);
                plusImageView.setId(buttonId);
                plusImageView.setOnClickListener(listener);
            } else {
                plusImageView.setVisibility(GONE);
            }
            return this;
        }

        public ListBuilder withClearButtonId(int buttonId, OnClickListener listener) {
            ImageView plusImageView = v.findViewById(R.id.bMinus);
            plusImageView.setId(buttonId);
            plusImageView.setOnClickListener(listener);
            return this;
        }

        public ListBuilder withAutoCompleteFilter(OnClickListener listener, int listId) {
            AutoCompleteTextView autoCompleteTxt = v.findViewById(R.id.autocomplete_filter);
            autoCompleteTxt.setFocusableInTouchMode(true);

            View view = v.findViewById(R.id.show_list);
            view.setId(listId);
            view.setOnClickListener(listener);

            return this;
        }

        public ListBuilder withoutMoreButton() {
            v.findViewById(R.id.more).setVisibility(GONE);
            return this;
        }
    }

    public class CheckBoxBuilder extends Builder {

        public CheckBoxBuilder(LinearLayout layout) {
            super(layout, R.layout.select_entry_checkbox);
        }

        public CheckBoxBuilder withCheckbox(boolean checked) {
            CheckBox checkBox = v.findViewById(R.id.checkbox);
            checkBox.setChecked(checked);
            return this;
        }

    }

}
