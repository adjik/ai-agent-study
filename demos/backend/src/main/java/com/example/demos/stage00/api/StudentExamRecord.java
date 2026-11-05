package com.example.demos.stage00.api;

import java.util.List;

public class StudentExamRecord {

    private String studentName;
    private String gradeLevel;
    private List<SubjectScore> subjectScores;

    public StudentExamRecord() {
    }

    public StudentExamRecord(String studentName, String gradeLevel, List<SubjectScore> subjectScores) {
        this.studentName = studentName;
        this.gradeLevel = gradeLevel;
        this.subjectScores = subjectScores;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(String gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public List<SubjectScore> getSubjectScores() {
        return subjectScores;
    }

    public void setSubjectScores(List<SubjectScore> subjectScores) {
        this.subjectScores = subjectScores;
    }
}
