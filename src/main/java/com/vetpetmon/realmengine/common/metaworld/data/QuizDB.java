package com.vetpetmon.realmengine.common.metaworld.data;

import com.vetpetmon.realmengine.RealmEngine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("unused") // This is a LIBRARY, we will use these later.
public class QuizDB extends SavedData {

    // Filename for the quiz database
    public static final String DB_FILENAME = "realmfall_quizquestions";
    protected List<QuestionEntry> data = new ArrayList<>();
    private static final ArrayList<QuestionEntry> questions = new ArrayList<>();
    private final boolean[] verifiedLayers = new boolean[4];

    protected static QuizDB client = new QuizDB();

    public static QuizDB get(LevelAccessor world) {
        if (world instanceof ServerLevel level)
            return level.getDataStorage().computeIfAbsent(QuizDB::load, QuizDB::new, DB_FILENAME);
        else return client;
    }

    // Get quiz questions
    public ArrayList<QuestionEntry> getQuestions() {
        return questions;
    }

    // get quiz questions count
    public int getQuestionsCount() {
        return getQuestions().size();
    }

    // Add a question to the database
    public void addQuestion(QuestionEntry question) {
        questions.add(question);
        this.setDirty();
    }

    // Get a question by ID
    public QuestionEntry getQuestionByID(int id) {
        for (QuestionEntry question : questions) {
            if (question.ID() == id) {
                return question;
            }
        }
        RealmEngine.LOGGER.warn("Question with ID {} not found in QuizDB.", id);
        return null;
    }

    // Get a random question by difficulty level
    public QuestionEntry getRandomQuestionByDifficulty(int difficultyLevel) {
        ArrayList<QuestionEntry> filteredQuestions = new ArrayList<>();
        for (QuestionEntry question : questions)
            if (question.difficultyLevel() == difficultyLevel)
                filteredQuestions.add(question);

        if (filteredQuestions.isEmpty()) {
            RealmEngine.LOGGER.warn("No questions found for difficulty level {} in QuizDB.", difficultyLevel);
            return null;
        }
        int randomIndex = (int) (Math.random() * filteredQuestions.size());
        return filteredQuestions.get(randomIndex);
    }



    public void syncData(LevelAccessor world) {
        this.setDirty();
        if (world instanceof Level level && !world.isClientSide())
            RealmEngine.PACKET_HANDLER.send(PacketDistributor.DIMENSION.with(level::dimension), new MessageSyncQuestionDB(save(new CompoundTag())));
    }


    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            sync((ServerPlayer) event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            sync((ServerPlayer) event.getEntity());
        }
    }

    public static void sync(ServerPlayer player) {
        SavedData worlddata = QuizDB.get(player.level());
        if (worlddata != null)
            RealmEngine.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
                    new MessageSyncQuestionDB(QuizDB
                            .get(player.level()).save(new CompoundTag())));
    }



    //Verify contents of QuizDB:
    // At least 60 questions for difficulty level 1
    // At least 30 questions for difficulty level 2
    // At least 30 questions for difficulty level 3
    // At least 20 questions for difficulty level 4
    // Print warning to log if not enough questions in a level, but still allow quiz to proceed.
    public void verifyQuizDBContents(QuizDB quizDB) {
        int level1Count = 0;
        int level2Count = 0;
        int level3Count = 0;
        int level4Count = 0;
        for (QuestionEntry question : quizDB.getQuestions()) {
            switch (question.difficultyLevel()) {
                case 1 -> level1Count++;
                case 2 -> level2Count++;
                case 3 -> level3Count++;
                case 4 -> level4Count++;
            }
        }
        String warnBase = "QuizDB does not contain enough questions for difficulty level {}, should be at least {}, found {}. Expect a lot of questions to be repeated!";
        if (level1Count < 60)
            RealmEngine.LOGGER.warn(warnBase, 1, 60, level1Count);
        if (level2Count < 30)
            RealmEngine.LOGGER.warn(warnBase, 2, 30, level2Count);
        if (level3Count < 30)
            RealmEngine.LOGGER.warn(warnBase, 3, 30, level3Count);
        if (level4Count < 20)
            RealmEngine.LOGGER.warn(warnBase, 4, 20, level4Count);
        // If a level has at least one question, mark it as verified
        if (level1Count > 0) verifiedLayers[0] = true;
        if (level2Count > 0) verifiedLayers[1] = true;
        if (level3Count > 0) verifiedLayers[2] = true;
        if (level4Count > 0) verifiedLayers[3] = true;
        if (hasInvalidLayer())
            RealmEngine.LOGGER.error("QuizDB verification incomplete: not all difficulty levels have questions. Spoiled Shade will go on strike!");
    }

    // Method to check if all layers are verified
    public boolean hasInvalidLayer() {
        for (boolean verified : verifiedLayers)
            if (!verified) return true;
        return false;
    }

    // Sanity check when loading from NBT
    private boolean isQuestionValid(QuestionEntry question) {
        int issuesFound = 0;
        String debugPrefix = "Invalid QuestionEntry ID " + question.ID() + ": ";
        // Negative ID
        if (question.ID() < 0) {
            RealmEngine.LOGGER.error("{} ID is negative.", debugPrefix);
            issuesFound++;
        }
        // Check for duplicate IDs
        for (QuestionEntry q : questions) {
            if (q != question && q.ID() == question.ID()) {
                RealmEngine.LOGGER.error("{} Duplicate ID found.", debugPrefix);
                issuesFound++;
            }
        }
        // empty question text
        if (question.question() == null || question.question().isEmpty()) {
            RealmEngine.LOGGER.error("{} Question text is empty.", debugPrefix);
            issuesFound++;
        }
        // null answers array
        if (question.answers() == null) {
            RealmEngine.LOGGER.error("{} Answers array is null.", debugPrefix);
            issuesFound++;
        }
        else {
            // not enough answers
            if (question.answers().length < 2) {
                RealmEngine.LOGGER.error("{} Not enough answer choices (minimum 2 required).", debugPrefix);
                issuesFound++;
            }
            // more than five answers
            if (question.answers().length > 5) {
                RealmEngine.LOGGER.error("{} Too many answer choices (maximum 5 allowed).", debugPrefix);
                issuesFound++;
            }
            // correct answer index out of bounds
            if (question.correctAnswerIndex() < 0 || question.correctAnswerIndex() >= question.answers().length) {
                RealmEngine.LOGGER.error("{} Correct answer index {} is out of bounds for {} answer choices.", debugPrefix, question.correctAnswerIndex(), question.answers().length);
                issuesFound++;
            }
        }
        // Difficulty out of range 0-4
        if (question.difficultyLevel() < 0 || question.difficultyLevel() > 4) {
            RealmEngine.LOGGER.error("{} Difficulty level {} is out of range (0-4).", debugPrefix, question.difficultyLevel());
            issuesFound++;
        }
        return issuesFound == 0;
    }

    public static QuizDB load(CompoundTag tag) {
        // Clear data before loading
        questions.clear();
        QuizDB data = new QuizDB();

        for (Tag t : tag.getList("Storage", Tag.TAG_COMPOUND)) {
            if (t instanceof CompoundTag ctag) {
                QuestionEntry entry = new QuestionEntry.Builder().fromNBT(ctag).build();
                // check if question is valid before adding
                if (!data.isQuestionValid(entry)) {
                    RealmEngine.LOGGER.warn("Skipping invalid QuestionEntry ID {}.", entry.ID());
                    continue;
                }
                data.data.add(entry);
//                RealmEngine.LOGGER.debug("Loaded QuestionEntry ID {} into QuizDB.", entry.ID());
                // log question details
//                RealmEngine.LOGGER.debug("{}", entry);
                questions.add(entry);
            }
        }
        //Debug log total questions loaded
        RealmEngine.LOGGER.info("Loaded {} QuestionEntries into QuizDB.", data.data.size());
        //Debug log all question IDs loaded
        StringBuilder ids = new StringBuilder("QuestionEntry IDs: ");
        for (QuestionEntry entry : data.data) {
            ids.append(entry.ID()).append(", ");
        }
//        RealmEngine.LOGGER.debug(ids.toString());

        // Verify contents of QuizDB
        data.verifyQuizDBContents(data);

        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag) {
        // Save QuestionEntries to NBT
        ListTag list = new ListTag();
        for (QuestionEntry question : questions)
            list.add(question.toNBT());
        compoundTag.put("Storage", list);
        return compoundTag;
    }

    public static class MessageSyncQuestionDB {
        private CompoundTag data = new CompoundTag();

        public MessageSyncQuestionDB() {}

        public MessageSyncQuestionDB(CompoundTag data) {
            this.data = data.copy();
        }

        public MessageSyncQuestionDB(FriendlyByteBuf buffer) {
            CompoundTag newData = buffer.readAnySizeNbt();
            if (newData != null) this.data = newData;
        }

        public static void write(MessageSyncQuestionDB msg, FriendlyByteBuf buf) {
            buf.writeNbt(msg.data);
        }

        public static void handle(MessageSyncQuestionDB msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                if (ctx.get().getDirection().getReceptionSide().isClient()) QuizDB.client = QuizDB.load(msg.data);
            });
            ctx.get().setPacketHandled(true);
        }
    }
}


