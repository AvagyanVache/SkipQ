package com.example.skipq.Fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

import com.example.skipq.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FAQFragment extends Fragment {

    private ExpandableListView faqExpandableListView;
    private FAQAdapter faqAdapter;
    private List<String> faqQuestions;
    private ImageView backButton;

    private Map<String, List<String>> faqAnswers;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.faq_fragment, container, false);
        backButton= view.findViewById(R.id.back);

        // Find the ExpandableListView
        faqExpandableListView = view.findViewById(R.id.faqExpandableListView);

        // Initialize FAQ data
        initializeFAQData();

        // Set up the adapter
        faqAdapter = new FAQAdapter(getContext(), faqQuestions, faqAnswers);
        faqExpandableListView.setAdapter(faqAdapter);

        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        faqExpandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                for (int i = 0; i < faqQuestions.size(); i++) {
                    if (i != groupPosition) {
                        faqExpandableListView.collapseGroup(i);
                    }
                }
            }
        });

        return view;
    }

    private void initializeFAQData() {
        // Initialize FAQ questions and answers (replace with your actual FAQs)
        faqQuestions = new ArrayList<>();
        faqAnswers = new HashMap<>();

        // Sample FAQ 1
        faqQuestions.add("How do I reset my password?");
        List<String> answer1 = new ArrayList<>();
        answer1.add("To reset your password, go to Profile -> Settings -> Change Password and click Reset Password. After that email an to change your passoword will be sent to you");
        faqAnswers.put(faqQuestions.get(0), answer1);

        // Sample FAQ 2
        faqQuestions.add("How can I contact support?");
        List<String> answer2 = new ArrayList<>();
        answer2.add("You can contact support by emailing awagyan.wache@gmail.com");
        faqAnswers.put(faqQuestions.get(1), answer2);


    faqQuestions.add("What is Preparation Time for each item?");
        List<String> answer3 = new ArrayList<>();
        answer3.add("Preparation time is the exact time it will take for your selected food to be ready. You can see it under every menu item details .");
        faqAnswers.put(faqQuestions.get(2), answer3);

    faqQuestions.add("Can I cancel my order?");
        List<String> answer4 = new ArrayList<>();
        answer4.add("Yes, you can cancel your order before it is confirmed by the restaurant.");
        faqAnswers.put(faqQuestions.get(3), answer4);

        faqQuestions.add("How can i know when my order will be ready?");
        List<String> answer5 = new ArrayList<>();
        answer5.add("After you place your order a live countdown appears showing the exact time left until your order will be ready.");
        faqAnswers.put(faqQuestions.get(4), answer5);

    }

    private static class FAQAdapter extends BaseExpandableListAdapter {
        private Context context;
        private List<String> questions;
        private Map<String, List<String>> answers;

        public FAQAdapter(Context context, List<String> questions, Map<String, List<String>> answers) {
            this.context = context;
            this.questions = questions;
            this.answers = answers;
        }

        @Override
        public int getGroupCount() {
            return questions.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return answers.get(questions.get(groupPosition)).size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return questions.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return answers.get(questions.get(groupPosition)).get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.simple_expandable_list_item_1, null);
            }

            TextView questionTextView = convertView.findViewById(android.R.id.text1);
            questionTextView.setText(questions.get(groupPosition));
            questionTextView.setTextSize(18);
            questionTextView.setPadding(32, 16, 32, 16);
            questionTextView.setTextColor(context.getResources().getColor(android.R.color.black));

            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.simple_list_item_1, null);
            }

            TextView answerTextView = convertView.findViewById(android.R.id.text1);
            answerTextView.setText(answers.get(questions.get(groupPosition)).get(childPosition));
            answerTextView.setTextSize(16);
            answerTextView.setPadding(48, 16, 32, 16);
            answerTextView.setTextColor(context.getResources().getColor(android.R.color.darker_gray));

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}