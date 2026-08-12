package com.example.base.ui.CustomerScreen; 

import java.util.Locale;

import com.example.base.service.FaqService;
import com.example.base.ui.MainScreen.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "bilgi-bankasi", layout = MainLayout.class)
@RolesAllowed({"CUSTOMER", "GODPANEL"})
@CssImport("./styles/customer/faq-view.css")
public class FaqView extends VerticalLayout implements HasDynamicTitle {

    private final FaqService faqService;
    private final Accordion faqAccordion = new Accordion();
    private final TextField searchField = new TextField();

    public FaqView(FaqService faqService) {
        this.faqService = faqService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);
        addClassName("faq-view-layout");

        Div container = new Div();
        container.addClassName("faq-container");

        H2 header = new H2(getTranslation("faq.headerTitle"));
        header.addClassName("faq-header");

        Paragraph subtext = new Paragraph(getTranslation("faq.headerSubtitle"));
        subtext.addClassName("faq-subtitle");

        searchField.setPlaceholder(getTranslation("faq.searchPlaceholder"));
        searchField.setWidthFull();
        searchField.setClearButtonVisible(true);
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> refreshFaq(e.getValue()));

        faqAccordion.addClassName("faq-accordion-container");
        refreshFaq("");

        container.add(header, subtext, searchField, faqAccordion);
        add(container);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("faq.pageTitle");
    }

    private void refreshFaq(String keyword) {
        faqAccordion.getChildren().toList().forEach(faqAccordion::remove);

        var allFaqs = faqService.searchFaq(""); 
        boolean isEnglish = "en".equals(UI.getCurrent().getLocale().getLanguage());

        String lowerKeyword = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);

        var filteredFaqs = allFaqs.stream().filter(faq -> {
            if (lowerKeyword.isBlank()) return true;
            
            String qTr = faq.getQuestion() != null ? faq.getQuestion().toLowerCase(Locale.ROOT) : "";
            String aTr = faq.getAnswer() != null ? faq.getAnswer().toLowerCase(Locale.ROOT) : "";
            
            String qEn = faq.getQuestionEn() != null ? faq.getQuestionEn().toLowerCase(Locale.ROOT) : "";
            String aEn = faq.getAnswerEn() != null ? faq.getAnswerEn().toLowerCase(Locale.ROOT) : "";
            
            return qTr.contains(lowerKeyword) || aTr.contains(lowerKeyword) || 
                   qEn.contains(lowerKeyword) || aEn.contains(lowerKeyword);
        }).toList();

        if (filteredFaqs.isEmpty()) {
            faqAccordion.add(
                getTranslation("faq.noResult.title"), 
                new Paragraph(getTranslation("faq.noResult.desc"))
            );
        } else {
            for (var faq : filteredFaqs) {
                String question = isEnglish && faq.getQuestionEn() != null && !faq.getQuestionEn().isBlank() 
                                  ? faq.getQuestionEn() 
                                  : faq.getQuestion();
                                  
                String answer = isEnglish && faq.getAnswerEn() != null && !faq.getAnswerEn().isBlank() 
                                ? faq.getAnswerEn() 
                                : faq.getAnswer();

                faqAccordion.add(question, new Paragraph(answer));
            }
        }
    }
}