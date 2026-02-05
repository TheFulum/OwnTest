package com.example.owntest.models;

public class TestWithCompletion {
    private Test test;
    private TestCompletion completion;

    public TestWithCompletion() {
    }

    public TestWithCompletion(Test test, TestCompletion completion) {
        this.test = test;
        this.completion = completion;
    }

    public Test getTest() {
        return test;
    }

    public void setTest(Test test) {
        this.test = test;
    }

    public TestCompletion getCompletion() {
        return completion;
    }

    public void setCompletion(TestCompletion completion) {
        this.completion = completion;
    }
}