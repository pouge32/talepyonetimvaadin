package com.example.base.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "xay_knowledge_base")
public class FaqEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "faq_seq")
    @SequenceGenerator(name = "faq_seq", sequenceName = "xay_faq_sequence", allocationSize = 1)
    private Integer id;
    
    private String question;
    
    @Column(length = 1000)
    private String answer;

    @Column(name = "question_en")
    private String questionEn;

    @Column(name = "answer_en")
    private String answerEn;

    public FaqEntity() {}
    public FaqEntity(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public Integer getId() { return id; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getQuestionEn() { return questionEn; }
    public void setQuestionEn(String questionEn) { this.questionEn = questionEn; }
    public String getAnswerEn() { return answerEn; }
    public void setAnswerEn(String answerEn) { this.answerEn = answerEn; }
}