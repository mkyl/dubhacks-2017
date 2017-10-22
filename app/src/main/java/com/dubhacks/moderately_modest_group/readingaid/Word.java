package com.dubhacks.moderately_modest_group.readingaid;

/**
 * Created by rober on 10/22/2017.
 */

public class Word implements Comparable<Word>{
    public final String word;
    public final double age;

    public Word(String word, double age) {this.word = word; this.age = age; }

    public int compareTo(Word other) {
        return Double.compare(other.age, this.age);
    }
}