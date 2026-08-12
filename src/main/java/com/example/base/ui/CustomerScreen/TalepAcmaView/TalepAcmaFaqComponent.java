package com.example.base.ui.CustomerScreen.TalepAcmaView;

import java.util.List;

import com.example.base.service.FaqService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class TalepAcmaFaqComponent extends Div {

    private final FaqService faqService;
    private final Span suggestionText = new Span();
    private final Paragraph suggestionAnswer = new Paragraph();

    public TalepAcmaFaqComponent(FaqService faqService) {
        this.faqService = faqService;
        
        setVisible(false);
        addClassName("talep-acma-suggestion-box");

        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon ideaIcon = VaadinIcon.LIGHTBULB.create();
        ideaIcon.setColor("var(--lumo-primary-color)");
        
        Span headerText = new Span(getTranslation("request.create.suggestion.header"));
        headerText.addClassName("talep-acma-suggestion-header");
        headerRow.add(ideaIcon, headerText);

        suggestionText.addClassName("talep-acma-suggestion-text");
        suggestionAnswer.addClassName("talep-acma-suggestion-answer");

        add(headerRow, suggestionText, suggestionAnswer);
    }

    public void checkForFaqSuggestion(String text) {
        if (text == null || text.trim().length() < 10) {
            setVisible(false);
            return;
        }

        List<String> userWords = extractMeaningfulWords(text);
        if (userWords.isEmpty()) {
            setVisible(false);
            return;
        }

        var allFaqs = faqService.searchFaq(""); 
        boolean isEnglish = "en".equals(UI.getCurrent().getLocale().getLanguage());
        
        int maxScore = 0;
        int bestIndex = -1; 

        for (int i = 0; i < allFaqs.size(); i++) {
            var faq = allFaqs.get(i);
            String q = isEnglish && faq.getQuestionEn() != null ? faq.getQuestionEn() : faq.getQuestion();
            String a = isEnglish && faq.getAnswerEn() != null ? faq.getAnswerEn() : faq.getAnswer();
            List<String> faqWords = extractMeaningfulWords(q + " " + a);
            int score = calculateMatchScore(userWords, faqWords);
            if (score > maxScore) {
                maxScore = score;
                bestIndex = i;
            }
        }

        if (bestIndex != -1 && maxScore > 0) {
            var topFaq = allFaqs.get(bestIndex); 
            String qDisplay = isEnglish && topFaq.getQuestionEn() != null ? topFaq.getQuestionEn() : topFaq.getQuestion();
            String aDisplay = isEnglish && topFaq.getAnswerEn() != null ? topFaq.getAnswerEn() : topFaq.getAnswer();

            suggestionText.setText(getTranslation("request.create.suggestion.question") + ": " + qDisplay);
            suggestionAnswer.setText(getTranslation("request.create.suggestion.answer") + ": " + aDisplay);
            setVisible(true);
        } else {
            setVisible(false);
        }
    }

    private List<String> extractMeaningfulWords(String text) {
        List<String> stopWords = List.of(
            "şirket", "şirkete", "şirketi", "nasıl", "neden", "niçin", "kim", 
            "hangi", "yapabilirim", "edebilirim", "istiyorum", "alabilirim", 
            "yardım", "lütfen", "için", "gibi", "kadar", "olan", "bana", 
            "benim", "bizim", "bunu", "veya", "ile", "göre", "acaba",
            "company", "how", "why", "who", "which", "want", "please", 
            "for", "like", "with", "about", "what", "can", "do"
        );

        return java.util.Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(w -> w.length() > 3 && !stopWords.contains(w))
                .toList();
    }

    private int calculateMatchScore(List<String> userWords, List<String> faqWords) {
        if (userWords.isEmpty() || faqWords.isEmpty()) return 0;
        java.util.Set<String> faqSet = new java.util.HashSet<>(faqWords);
        int matchCount = 0;
        for (String w : userWords) {
            if (faqSet.contains(w)) {
                matchCount++;
            }
        }
        return matchCount; 
    }
}