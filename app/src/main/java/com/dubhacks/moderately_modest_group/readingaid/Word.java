package com.dubhacks.moderately_modest_group.readingaid;

/**
 * Created by rober on 10/22/2017.
 */

public class Word implements Comparable<Word>{
    public final String word;
    public final Double age;

    public Word(String word, Double age) {this.word = word; this.age = age; }

    public int compareTo(Word other) {
        if (this.age == null) {
            return 1;
        }

        if (other.age == null) {
            return -1;
        }

        return Double.compare(other.age, this.age);
    }
}