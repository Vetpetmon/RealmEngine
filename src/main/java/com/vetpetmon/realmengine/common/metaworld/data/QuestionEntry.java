package com.vetpetmon.realmengine.common.metaworld.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.jetbrains.annotations.NotNull;

// Class for quiz question entries and answers
@SuppressWarnings("unused") // This is a LIBRARY, we will use these later.
public record QuestionEntry(int ID, String question, String[] answers, int correctAnswerIndex, int difficultyLevel) {

        // prints the question entry in a readable format
        @Override
        public @NotNull String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ID: ").append(ID).append("\n");
            sb.append("Question: ").append(question).append("\n");
            sb.append("Answers:\n");
            for (int i = 0; i < answers.length; i++)
                sb.append(i).append(": ").append(answers[i]).append("\n");

            sb.append("Correct Answer Index: ").append(correctAnswerIndex).append("\n");
            sb.append("Difficulty Level: ").append(difficultyLevel).append("\n");
            return sb.toString();
        }
        
        // From NBT
        public QuestionEntry fromNBT(CompoundTag nbt) {
            int id = nbt.getInt("ID");
            String question = nbt.getString("question");
            int difficultyLevel = nbt.getInt("difficultyLevel");
            int correctAnswerIndex = nbt.getInt("correctAnswerIndex");
            ListTag answersList = nbt.getList("answers", 8); // 8 is the ID for String
            String[] answers = new String[answersList.size()];
            for (int i = 0; i < answersList.size(); i++)
                answers[i] = answersList.getString(i);
            return new QuestionEntry(id, question, answers, correctAnswerIndex, difficultyLevel);
        }
        
        // Save to NBT
        public CompoundTag toNBT() {
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("ID", ID);
            nbt.putString("question", question);
            nbt.putInt("difficultyLevel", difficultyLevel);
            nbt.putInt("correctAnswerIndex", correctAnswerIndex);
            ListTag answersList = new ListTag();
            for (String answer : answers)
                answersList.add(StringTag.valueOf(answer));
            nbt.put("answers", answersList);
            return nbt;
        }



    // Builder class for QuestionEntry
        public static class Builder {
            private int ID = 0;
            private String question;
            private String[] answers;
            private int correctAnswerIndex, difficultyLevel = 1;

            public Builder setID(int id) {
                this.ID = id;
                return this;
            }

            public Builder setQuestion(String question) {
                this.question = question;
                return this;
            }

            public Builder setAnswers(String[] answers) {
                this.answers = answers;
                return this;
            }

            public Builder setCorrectAnswerIndex(int index) {
                this.correctAnswerIndex = index;
                return this;
            }

            public Builder setDifficultyLevel(int level) {
                this.difficultyLevel = level;
                return this;
            }

            // From NBT
            public Builder fromNBT(CompoundTag nbt) {
                this.ID = nbt.getInt("ID");
                this.question = nbt.getString("question");
                this.difficultyLevel = nbt.getInt("difficultyLevel");
                this.correctAnswerIndex = nbt.getInt("correctAnswerIndex");
                ListTag answersList = nbt.getList("answers", 8); // 8 is the ID for String
                this.answers = new String[answersList.size()];
                for (int i = 0; i < answersList.size(); i++)
                    this.answers[i] = answersList.getString(i);
                return this;
            }

            public QuestionEntry build() {
                return new QuestionEntry(ID, question, answers, correctAnswerIndex, difficultyLevel);
            }
        }
    }
