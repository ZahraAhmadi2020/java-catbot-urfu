package com.urfu.catbot.telegram.bot;

import com.urfu.catbot.shared.dto.AddCatCommand;
import com.urfu.catbot.shared.dto.AddCommentCommand;
import com.urfu.catbot.shared.dto.AddReactionCommand;
import com.urfu.catbot.telegram.messaging.CatCommandProducer;
import com.urfu.catbot.telegram.service.UserLanguageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

@Component
public class SimpleTelegramBot extends TelegramLongPollingBot {

  private final String botToken;
  private final String botUsername;
  private final MessageSource messageSource;
  private final UserLanguageService userLanguageService;
  private final CatCommandProducer catCommandProducer;

  private final List<CatSample> userAddedCats = new CopyOnWriteArrayList<>();
  private final Map<Long, Map<Long, Map<String, Integer>>> catReactionsPreview = new HashMap<>();
  private final Map<Long, List<Comment>> catCommentsPreview = new HashMap<>();

  public SimpleTelegramBot(
      @Value("${telegram.bot.token}") String botToken,
      @Value("${telegram.bot.username}") String botUsername,
      MessageSource messageSource,
      UserLanguageService userLanguageService,
      CatCommandProducer catCommandProducer) {
    this.botToken = botToken;
    this.botUsername = botUsername;
    this.messageSource = messageSource;
    this.userLanguageService = userLanguageService;
    this.catCommandProducer = catCommandProducer;
    System.out.println("🤖 Bot initialized: @" + botUsername);
  }

  @Override
  public String getBotToken() {
    return botToken;
  }

  @Override
  public String getBotUsername() {
    return botUsername;
  }

  @Override
  public void onUpdateReceived(Update update) {
    try {
      if (update.hasMessage()) {
        if (update.getMessage().hasPhoto()) {
          handlePhotoMessage(update);
          return;
        }
        if (update.getMessage().hasText()) {
          handleTextMessage(update);
          return;
        }
      }

      if (update.hasCallbackQuery()) {
        handleCallbackQuery(update);
        return;
      }
    } catch (Exception e) {
      System.err.println("❌ Error: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void handleTextMessage(Update update) {
    long chatId = update.getMessage().getChatId();
    long userId = update.getMessage().getFrom().getId();
    String text = update.getMessage().getText().trim();
    String username = update.getMessage().getFrom().getUserName() != null ? update.getMessage().getFrom().getUserName()
        : "user" + userId;

    if (!userLanguageService.hasSelectedLanguage(userId)) {
      showLanguageSelection(chatId, userId);
      return;
    }

    if ("/start".equalsIgnoreCase(text)) {
      showMainMenu(chatId, userId);
      return;
    }

    String state = userLanguageService.getState(userId);
    String langCode = userLanguageService.getLanguage(userId);

    if (state != null && state.startsWith("AWAITING_COMMENT:")) {
      long catIndex = Long.parseLong(state.split(":")[1]);

      if (text.length() < 2) {
        sendReply(chatId, userId, "comment.add.error.too_short");
        return;
      }

      addCommentPreview(catIndex, userId, username, text);
      catCommandProducer.sendAddComment(new AddCommentCommand(catIndex, userId, username, text));
      userLanguageService.clearUserData(userId);
      showComments(chatId, userId, catIndex, true);
      return;
    }

    if ("AWAITING_NAME".equals(state)) {
      UserLanguageService.CatData catData = userLanguageService.getCatData(userId);
      if (catData != null) {
        if (text.length() < 2) {
          sendReply(chatId, userId, "cat.add.error.name_too_short");
          return;
        }
        catData.setName(text);
        userLanguageService.setState(userId, "AWAITING_COLOR");
        sendReply(chatId, userId, "cat.add.prompt_color");
      }
    } else if ("AWAITING_COLOR".equals(state)) {
      UserLanguageService.CatData catData = userLanguageService.getCatData(userId);
      if (catData != null) {
        catData.setColor(text);
        userLanguageService.setState(userId, "AWAITING_BREED");
        sendReply(chatId, userId, "cat.add.prompt_breed");
      }
    } else if ("AWAITING_BREED".equals(state)) {
      UserLanguageService.CatData catData = userLanguageService.getCatData(userId);
      if (catData != null) {
        catData.setBreed(text);
        userLanguageService.setState(userId, "AWAITING_AGE");
        sendReply(chatId, userId, "cat.add.prompt_age");
      }
    } else if ("AWAITING_AGE".equals(state)) {
      UserLanguageService.CatData catData = userLanguageService.getCatData(userId);
      if (catData != null) {
        if (!Pattern.matches("^\\d+$", text.trim())) {
          sendReply(chatId, userId, "cat.add.error.invalid_age");
          sendReply(chatId, userId, "cat.add.prompt_age");
          return;
        }

        try {
          int age = Integer.parseInt(text.trim());
          if (age <= 0 || age > 300) {
            sendReply(chatId, userId, "cat.add.error.invalid_age_range");
            sendReply(chatId, userId, "cat.add.prompt_age");
          } else {
            catData.setAge(age);
            userLanguageService.setState(userId, "AWAITING_DESCRIPTION");
            sendReply(chatId, userId, "cat.add.prompt_description");
          }
        } catch (NumberFormatException e) {
          sendReply(chatId, userId, "cat.add.error.invalid_age");
          sendReply(chatId, userId, "cat.add.prompt_age");
        }
      }
    } else if ("AWAITING_DESCRIPTION".equals(state)) {
      UserLanguageService.CatData catData = userLanguageService.getCatData(userId);
      if (catData != null) {
        catData.setDescription(text.isEmpty() ? getMessage("cat.default_description", langCode) : text);

        CatSample newCat = new CatSample(
            catData.getName(),
            catData.getPhotoFileId(),
            catData.getColor(),
            catData.getBreed(),
            catData.getAge(),
            catData.getDescription());
        userAddedCats.add(newCat);
        long newCatIndex = getSampleCatsByLanguage(langCode).size() + userAddedCats.size() - 1;

        catCommandProducer.sendAddCat(new AddCatCommand(
            userId, username, catData.getPhotoFileId(),
            catData.getName(), catData.getColor(),
            catData.getBreed(), catData.getAge(),
            catData.getDescription()));

        System.out.println("✅ گربه جدید اضافه شد: " + catData.getName() + " (ایندکس: " + newCatIndex + ")");
        userLanguageService.clearUserData(userId);
        sendReply(chatId, userId, "cat.add.success");
        viewCat(chatId, userId, (int) newCatIndex);
      }
    }
  }

  private void handlePhotoMessage(Update update) {
    long chatId = update.getMessage().getChatId();
    long userId = update.getMessage().getFrom().getId();

    String state = userLanguageService.getState(userId);

    if ("AWAITING_PHOTO".equals(state) && !update.getMessage().getPhoto().isEmpty()) {
      String fileId = update.getMessage().getPhoto().get(update.getMessage().getPhoto().size() - 1).getFileId();
      UserLanguageService.CatData catData = new UserLanguageService.CatData();
      catData.setPhotoFileId(fileId);
      userLanguageService.setCatData(userId, catData);
      userLanguageService.setState(userId, "AWAITING_NAME");
      sendReply(chatId, userId, "cat.add.prompt_name");
      return;
    }

    if (!userLanguageService.hasSelectedLanguage(userId)) {
      showLanguageSelection(chatId, userId);
      return;
    }
  }

  private void handleCallbackQuery(Update update) {
    String callbackData = update.getCallbackQuery().getData();
    long chatId = update.getCallbackQuery().getMessage().getChatId();
    long userId = update.getCallbackQuery().getFrom().getId();
    int messageId = update.getCallbackQuery().getMessage().getMessageId();

    try {
      execute(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.builder()
          .callbackQueryId(update.getCallbackQuery().getId())
          .build());
    } catch (TelegramApiException e) {
    }

    String langCode = userLanguageService.getLanguage(userId);

    if (callbackData.startsWith("lang_")) {
      String languageCode = callbackData.substring(5);
      userLanguageService.setLanguage(userId, languageCode);
      showMainMenu(chatId, userId);
    } else if ("menu_view".equals(callbackData)) {
      viewCat(chatId, userId, 0);
    } else if ("menu_add".equals(callbackData)) {
      userLanguageService.setState(userId, "AWAITING_PHOTO");
      sendReply(chatId, userId, "cat.add.prompt_photo");
    } else if ("menu_change_lang".equals(callbackData)) {
      showLanguageSelection(chatId, userId);
    } else if (callbackData.startsWith("cat_")) {
      String[] parts = callbackData.split("_");
      int catIndex = Integer.parseInt(parts[1]);
      if ("next".equals(parts[2])) {
        viewCat(chatId, userId, catIndex + 1);
      } else if ("prev".equals(parts[2])) {
        viewCat(chatId, userId, Math.max(0, catIndex - 1));
      } else if ("back".equals(parts[2])) {
        showMainMenu(chatId, userId);
      } else if ("view".equals(parts[2])) {
        viewCat(chatId, userId, catIndex);
      }
    } else if (callbackData.startsWith("react_")) {
      String[] parts = callbackData.split("_");
      long catIndex = Long.parseLong(parts[1]);
      String emoji = parts[2];

      addReactionPreview(catIndex, userId, emoji);
      catCommandProducer.sendAddReaction(new AddReactionCommand(catIndex, userId, "user" + userId, emoji));
      editCatMessage(chatId, messageId, userId, catIndex);
    } else if (callbackData.startsWith("add_comment_")) {
      long catIndex = Long.parseLong(callbackData.split("_")[2]);
      userLanguageService.setState(userId, "AWAITING_COMMENT:" + catIndex);
      sendReply(chatId, userId, "comment.add.prompt");
    } else if ("back_to_menu".equals(callbackData)) {
      showMainMenu(chatId, userId);
    }
  }

  private void showLanguageSelection(long chatId, long userId) {
    SendMessage message = new SendMessage();
    message.setChatId(String.valueOf(chatId));
    message.setText(
        "🇮🇷 لطفاً زبان خود را انتخاب کنید:\n" +
            "🇬🇧 Please select your language:\n" +
            "🇷🇺 Пожалуйста, выберите язык:");

    InlineKeyboardButton btnFa = new InlineKeyboardButton("🇮🇷 فارسی");
    btnFa.setCallbackData("lang_fa");
    InlineKeyboardButton btnEn = new InlineKeyboardButton("🇬🇧 English");
    btnEn.setCallbackData("lang_en");
    InlineKeyboardButton btnRu = new InlineKeyboardButton("🇷🇺 Русский");
    btnRu.setCallbackData("lang_ru");

    List<InlineKeyboardButton> row1 = Arrays.asList(btnFa, btnEn);
    List<InlineKeyboardButton> row2 = Collections.singletonList(btnRu);
    InlineKeyboardMarkup markup = new InlineKeyboardMarkup(Arrays.asList(row1, row2));
    message.setReplyMarkup(markup);

    try {
      execute(message);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  // main menu
  private void showMainMenu(long chatId, long userId) {
    String langCode = userLanguageService.getLanguage(userId);
    Locale locale = Locale.forLanguageTag(langCode);
    LocaleContextHolder.setLocale(locale);

    try {
      SendMessage message = new SendMessage();
      message.setChatId(String.valueOf(chatId));
      message.setText(getMessage("bot.start.welcome", langCode));

      InlineKeyboardButton btnView = new InlineKeyboardButton(
          "🐱 " + getMessage("menu.view_cats", langCode));
      btnView.setCallbackData("menu_view");

      InlineKeyboardButton btnAdd = new InlineKeyboardButton(
          "➕ " + getMessage("menu.add_cat", langCode));
      btnAdd.setCallbackData("menu_add");

      InlineKeyboardButton btnChangeLang = new InlineKeyboardButton(
          "🌐 " + getMessage("menu.change_language", langCode));
      btnChangeLang.setCallbackData("menu_change_lang");

      List<InlineKeyboardButton> row1 = Arrays.asList(btnView, btnAdd);
      List<InlineKeyboardButton> row2 = Collections.singletonList(btnChangeLang);
      InlineKeyboardMarkup markup = new InlineKeyboardMarkup(Arrays.asList(row1, row2));
      message.setReplyMarkup(markup);

      execute(message);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    } finally {
      LocaleContextHolder.resetLocaleContext();
    }
  }

  // view cat
  private void viewCat(long chatId, long userId, int catIndex) {
    String langCode = userLanguageService.getLanguage(userId);
    List<CatSample> allCats = getAllCats(langCode);

    if (catIndex < 0 || catIndex >= allCats.size()) {
      catIndex = 0;
    }

    CatSample cat = allCats.get(catIndex);
    Map<String, Integer> reactions = getReactionsPreview((long) catIndex);
    List<Comment> comments = getCommentsPreview((long) catIndex);

    StringBuilder reactionsText = new StringBuilder();
    if (reactions.containsKey("❤️"))
      reactionsText.append("❤️ ").append(reactions.get("❤️")).append(" ");
    if (reactions.containsKey("🔥"))
      reactionsText.append("🔥 ").append(reactions.get("🔥")).append(" ");
    if (reactions.containsKey("😲"))
      reactionsText.append("😲 ").append(reactions.get("😲")).append(" ");
    if (reactions.containsKey("👍"))
      reactionsText.append("👍 ").append(reactions.get("👍")).append(" ");
    if (reactions.containsKey("👎"))
      reactionsText.append("👎 ").append(reactions.get("👎")).append(" ");
    if (reactionsText.length() == 0)
      reactionsText.append(getMessage("cat.no_reactions", langCode));

    String caption = String.format(
        "🐱 <b>%s</b>\n\n" +
            "%s %s | %s %d %s\n" +
            "%s %s\n\n" +
            "%s %s\n\n" +
            "━━━━━━━━━━━━━━━━━━\n" +
            "%s",
        cat.name,
        getMessage("cat.details.color_label", langCode), cat.color,
        getMessage("cat.details.age_label", langCode), cat.age, getMessage("cat.details.age_unit", langCode),
        getMessage("cat.details.breed_label", langCode), cat.breed,
        getMessage("cat.details.description_label", langCode), cat.description,
        reactionsText.toString().trim());

    SendPhoto sendPhoto = new SendPhoto();
    sendPhoto.setChatId(String.valueOf(chatId));
    sendPhoto.setPhoto(new InputFile(cat.photoFileId));
    sendPhoto.setCaption(caption);
    sendPhoto.setParseMode("HTML");

    InlineKeyboardButton btnHeart = new InlineKeyboardButton("❤️");
    btnHeart.setCallbackData("react_" + catIndex + "_❤️");
    InlineKeyboardButton btnFire = new InlineKeyboardButton("🔥");
    btnFire.setCallbackData("react_" + catIndex + "_🔥");
    InlineKeyboardButton btnSurprised = new InlineKeyboardButton("😲");
    btnSurprised.setCallbackData("react_" + catIndex + "_😲");

    InlineKeyboardButton btnAddComment = new InlineKeyboardButton(
        getMessage("cat.add_comment.button", langCode) + " (" + comments.size() + ")");
    btnAddComment.setCallbackData("add_comment_" + catIndex);

    InlineKeyboardButton btnPrev = new InlineKeyboardButton(getMessage("cat.nav.previous", langCode));
    btnPrev.setCallbackData(catIndex == 0 ? "noop" : "cat_" + (catIndex - 1) + "_prev");
    InlineKeyboardButton btnNext = new InlineKeyboardButton(getMessage("cat.nav.next", langCode));
    btnNext.setCallbackData(catIndex == allCats.size() - 1 ? "noop" : "cat_" + (catIndex + 1) + "_next");
    InlineKeyboardButton btnBack = new InlineKeyboardButton(getMessage("cat.nav.back", langCode));
    btnBack.setCallbackData("cat_" + catIndex + "_back");

    List<List<InlineKeyboardButton>> rows = new ArrayList<>();
    rows.add(Arrays.asList(btnHeart, btnFire, btnSurprised));
    rows.add(Collections.singletonList(btnAddComment));
    rows.add(Arrays.asList(btnPrev, btnNext));
    rows.add(Collections.singletonList(btnBack));

    InlineKeyboardMarkup markup = new InlineKeyboardMarkup(rows);
    sendPhoto.setReplyMarkup(markup);

    try {
      execute(sendPhoto);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  private void editCatMessage(long chatId, int messageId, long userId, long catId) {
    String langCode = userLanguageService.getLanguage(userId);
    List<CatSample> allCats = getAllCats(langCode);

    if (catId < 0 || catId >= allCats.size())
      return;

    CatSample cat = allCats.get((int) catId);
    Map<String, Integer> reactions = getReactionsPreview(catId);
    List<Comment> comments = getCommentsPreview(catId);

    StringBuilder reactionsText = new StringBuilder();
    if (reactions.containsKey("❤️"))
      reactionsText.append("❤️ ").append(reactions.get("❤️")).append(" ");
    if (reactions.containsKey("🔥"))
      reactionsText.append("🔥 ").append(reactions.get("🔥")).append(" ");
    if (reactions.containsKey("😲"))
      reactionsText.append("😲 ").append(reactions.get("😲")).append(" ");
    if (reactions.containsKey("👍"))
      reactionsText.append("👍 ").append(reactions.get("👍")).append(" ");
    if (reactions.containsKey("👎"))
      reactionsText.append("👎 ").append(reactions.get("👎")).append(" ");
    if (reactionsText.length() == 0)
      reactionsText.append(getMessage("cat.no_reactions", langCode));

    String caption = String.format(
        "🐱 <b>%s</b>\n\n" +
            "%s %s | %s %d %s\n" +
            "%s %s\n\n" +
            "%s %s\n\n" +
            "━━━━━━━━━━━━━━━━━━\n" +
            "%s",
        cat.name,
        getMessage("cat.details.color_label", langCode), cat.color,
        getMessage("cat.details.age_label", langCode), cat.age, getMessage("cat.details.age_unit", langCode),
        getMessage("cat.details.breed_label", langCode), cat.breed,
        getMessage("cat.details.description_label", langCode), cat.description,
        reactionsText.toString().trim());

    try {
      EditMessageCaption editCaption = new EditMessageCaption();
      editCaption.setChatId(String.valueOf(chatId));
      editCaption.setMessageId(messageId);
      editCaption.setCaption(caption);
      editCaption.setParseMode("HTML");

      InlineKeyboardButton btnHeart = new InlineKeyboardButton("❤️");
      btnHeart.setCallbackData("react_" + catId + "_❤️");
      InlineKeyboardButton btnFire = new InlineKeyboardButton("🔥");
      btnFire.setCallbackData("react_" + catId + "_🔥");
      InlineKeyboardButton btnSurprised = new InlineKeyboardButton("😲");
      btnSurprised.setCallbackData("react_" + catId + "_😲");

      InlineKeyboardButton btnAddComment = new InlineKeyboardButton(
          getMessage("cat.add_comment.button", langCode) + " (" + comments.size() + ")");
      btnAddComment.setCallbackData("add_comment_" + catId);

      InlineKeyboardButton btnPrev = new InlineKeyboardButton(getMessage("cat.nav.previous", langCode));
      btnPrev.setCallbackData((int) catId == 0 ? "noop" : "cat_" + ((int) catId - 1) + "_prev");
      InlineKeyboardButton btnNext = new InlineKeyboardButton(getMessage("cat.nav.next", langCode));
      btnNext.setCallbackData((int) catId == allCats.size() - 1 ? "noop" : "cat_" + ((int) catId + 1) + "_next");
      InlineKeyboardButton btnBack = new InlineKeyboardButton(getMessage("cat.nav.back", langCode));
      btnBack.setCallbackData("cat_" + catId + "_back");

      List<List<InlineKeyboardButton>> rows = new ArrayList<>();
      rows.add(Arrays.asList(btnHeart, btnFire, btnSurprised));
      rows.add(Collections.singletonList(btnAddComment));
      rows.add(Arrays.asList(btnPrev, btnNext));
      rows.add(Collections.singletonList(btnBack));

      InlineKeyboardMarkup markup = new InlineKeyboardMarkup(rows);
      editCaption.setReplyMarkup(markup);

      execute(editCaption);
    } catch (TelegramApiException e) {
      viewCat(chatId, userId, (int) catId);
    }
  }

  private void showComments(long chatId, long userId, long catId, boolean showAfterComment) {
    String langCode = userLanguageService.getLanguage(userId);
    List<CatSample> allCats = getAllCats(langCode);
    CatSample cat = allCats.get((int) catId);
    List<Comment> comments = getCommentsPreview(catId);

    StringBuilder commentsText = new StringBuilder();
    commentsText.append("💬 <b>");
    commentsText.append(getMessage("cat.comments.title", langCode).replace("{catName}", cat.name));
    commentsText.append("</b>\n\n");

    if (comments.isEmpty()) {
      commentsText.append(getMessage("cat.comments.empty", langCode));
    } else {
      for (int i = 0; i < Math.min(comments.size(), 10); i++) {
        Comment comment = comments.get(i);
        commentsText.append(String.format(
            "%d. 👤 <b>%s</b>:\n   %s\n\n",
            i + 1,
            comment.username,
            comment.text));
      }
      if (comments.size() > 10) {
        commentsText.append(String.format(getMessage("cat.comments.more", langCode), comments.size() - 10));
      }
    }

    commentsText.append("\n━━━━━━━━━━━━━━━━━━\n");
    if (showAfterComment) {
      commentsText.append("✅ ").append(getMessage("comment.add.success", langCode)).append("\n\n");
    }

    SendMessage message = new SendMessage();
    message.setChatId(String.valueOf(chatId));
    message.setText(commentsText.toString());
    message.setParseMode("HTML");

    // back to menu button
    InlineKeyboardButton btnBack = new InlineKeyboardButton(getMessage("cat.nav.back", langCode));
    btnBack.setCallbackData("back_to_menu");

    InlineKeyboardMarkup markup = new InlineKeyboardMarkup(
        Collections.singletonList(Collections.singletonList(btnBack)));
    message.setReplyMarkup(markup);

    try {
      execute(message);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  private String getMessage(String key, String langCode) {
    Locale locale = Locale.forLanguageTag(langCode);
    LocaleContextHolder.setLocale(locale);
    try {
      return messageSource.getMessage(key, null, locale);
    } catch (Exception e) {
      return "❌ Missing translation: " + key;
    } finally {
      LocaleContextHolder.resetLocaleContext();
    }
  }

  private void addReactionPreview(long catId, long userId, String emoji) {
    Map<Long, Map<String, Integer>> catMap = catReactionsPreview.computeIfAbsent(catId, k -> new HashMap<>());
    Map<String, Integer> userReactions = catMap.computeIfAbsent(userId, k -> new HashMap<>());
    userReactions.clear();
    userReactions.put(emoji, 1);
  }

  private Map<String, Integer> getReactionsPreview(Long catId) {
    Map<String, Integer> result = new HashMap<>();
    Map<Long, Map<String, Integer>> catMap = catReactionsPreview.getOrDefault(catId, new HashMap<>());
    for (Map<String, Integer> userReactions : catMap.values()) {
      for (Map.Entry<String, Integer> entry : userReactions.entrySet()) {
        result.put(entry.getKey(), result.getOrDefault(entry.getKey(), 0) + entry.getValue());
      }
    }
    return result;
  }

  private void addCommentPreview(long catId, long userId, String username, String text) {
    List<Comment> comments = catCommentsPreview.computeIfAbsent(catId, k -> new CopyOnWriteArrayList<>());
    comments.add(new Comment(username, text.substring(0, Math.min(text.length(), 200))));
  }

  private List<Comment> getCommentsPreview(Long catId) {
    return catCommentsPreview.getOrDefault(catId, new ArrayList<>());
  }

  private void sendReply(long chatId, long userId, String messageKey) {
    String lang = userLanguageService.getLanguage(userId);
    Locale locale = Locale.forLanguageTag(lang);
    LocaleContextHolder.setLocale(locale);
    String text = messageSource.getMessage(messageKey, null, locale);
    sendReply(chatId, text);
    LocaleContextHolder.resetLocaleContext();
  }

  private void sendReply(long chatId, String text) {
    try {
      execute(SendMessage.builder()
          .chatId(String.valueOf(chatId))
          .text(text)
          .build());
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  // cat sample show
  private static class CatSample {
    String name, photoFileId, color, breed, description;
    int age;

    CatSample(String n, String fid, String c, String b, int a, String d) {
      name = n;
      photoFileId = fid;
      color = c;
      breed = b;
      age = a;
      description = d;
    }
  }

  private static class Comment {
    String username, text;

    Comment(String u, String t) {
      username = u;
      text = t;
    }
  }

  private List<CatSample> getAllCats(String langCode) {
    List<CatSample> cats = getSampleCatsByLanguage(langCode);
    cats.addAll(userAddedCats);
    return cats;
  }

  private List<CatSample> getSampleCatsByLanguage(String langCode) {
    switch (langCode) {
      case "en":
        return getSampleCatsEn();
      case "ru":
        return getSampleCatsRu();
      default:
        return getSampleCatsFa();
    }
  }

  // cat list
  private List<CatSample> getSampleCatsFa() {
    List<CatSample> cats = new ArrayList<>();
    cats.add(
        new CatSample("سنوی", "AgACAgQAAxkBAAIBKGmF4IoouqCLxqYndAAB_DnL9gK8gwACqg1rGwOtMFB5GlSH3Vy-CAEAAwIAA20AAzgE",
            "سفید", "پرشین", 12, "گربه بسیار شیرین و بازیگوش با چشمان آبی که عاشق بازی با توپ است"));
    cats.add(
        new CatSample("تیگر", "AgACAgQAAxkBAAIBKmmF4M10GCQONvA7gjOaKCa5jKhPAAKrDWsbA60wUH_xNATbI4aKAQADAgADbQADOAQ",
            "نارنجی", "مخلوط", 8, "گربه پرانرژی و دوست‌داشتنی، همیشه در حال کشف محیط جدید است"));
    cats.add(
        new CatSample("اشکان", "AgACAgQAAxkBAAIBEWmF4CIBPeRaIz55cG_1mrEwGUWoAAKoDWsbA60wUDrGnal3BW73AQADAgADbQADOAQ",
            "خاکستری", "بریتیش شورت‌هیر", 24, "گربه آرام و مهربان، عاشق خوابیدن کنار پنجره و تماشای پرندگان"));
    cats.add(
        new CatSample("لیلا", "AgACAgQAAxkBAAIBMmmF4Xb7AW_lcj98Q8cB8BKnZ0HtAAKtDWsbA60wUN9HXPXJGW2KAQADAgADbQADOAQ",
            "مشکی", "سیامی", 6, "گربه کوچک و بازیگوش با چشمان آبی درخشان"));
    cats.add(
        new CatSample("بابک", "AgACAgQAAxkBAAIBNWmF4Y-KYabARQ5DxDWd2uNtJuScAAKuDWsbA60wUFs0zrujt6-aAQADAgADbQADOAQ",
            "زرد", "مخلوط", 18, "گربه بامزه و دوست‌داشتنی که عاشق ماهی است"));
    cats.add(
        new CatSample("نیکو", "AgACAgQAAxkBAAIBOGmF4f9OQDX7KtH7dLb5VuTZI_6DAAKvDWsbA60wUBYsk4WBsiG_AQADAgADeAADOAQ",
            "نارنجی", "پرشین", 10, "گربه شیطنت‌کار با رنگ نارنجی درخشان"));
    cats.add(
        new CatSample("پارسا", "AgACAgQAAxkBAAIBO2mF4iPSibwDWARcaQ3lUOKrM6JNAAKwDWsbA60wUPmfyngt4OoyAQADAgADeQADOAQ",
            "سفید و خاکستری", "بریتیش", 15, "گربه آرام با چشمان سبز درخشان"));
    cats.add(
        new CatSample("یلدا", "AgACAgQAAxkBAAIBPmmF4j6wZGZToXhy7ZTpqZqXw7gpAAKxDWsbA60wUNnd9LgiCe4XAQADAgADeAADOAQ",
            "خال‌خالی", "مخلوط", 9, "گربه باهوش و کنجکاو با خال‌های زیبا"));
    return cats;
  }

  private List<CatSample> getSampleCatsEn() {
    List<CatSample> cats = new ArrayList<>();
    cats.add(
        new CatSample("Snowy", "AgACAgQAAxkBAAIBKGmF4IoouqCLxqYndAAB_DnL9gK8gwACqg1rGwOtMFB5GlSH3Vy-CAEAAwIAA20AAzgE",
            "White", "Persian", 12, "Very cute and playful cat with blue eyes who loves playing with balls"));
    cats.add(
        new CatSample("Tiger", "AgACAgQAAxkBAAIBKmmF4M10GCQONvA7gjOaKCa5jKhPAAKrDWsbA60wUH_xNATbI4aKAQADAgADbQADOAQ",
            "Orange", "Mixed", 8, "Energetic and lovely cat, always exploring new environments"));
    cats.add(
        new CatSample("Ashkan", "AgACAgQAAxkBAAIBEWmF4CIBPeRaIz55cG_1mrEwGUWoAAKoDWsbA60wUDrGnal3BW73AQADAgADbQADOAQ",
            "Gray", "British Shorthair", 24, "Calm and friendly cat, loves sleeping by the window and watching birds"));
    cats.add(
        new CatSample("Lila", "AgACAgQAAxkBAAIBMmmF4Xb7AW_lcj98Q8cB8BKnZ0HtAAKtDWsbA60wUN9HXPXJGW2KAQADAgADbQADOAQ",
            "Black", "Siamese", 6, "Small playful cat with bright blue eyes"));
    cats.add(
        new CatSample("Babak", "AgACAgQAAxkBAAIBNWmF4Y-KYabARQ5DxDWd2uNtJuScAAKuDWsbA60wUFs0zrujt6-aAQADAgADbQADOAQ",
            "Yellow", "Mixed", 18, "Funny and lovely cat who loves fish"));
    cats.add(
        new CatSample("Niko", "AgACAgQAAxkBAAIBOGmF4f9OQDX7KtH7dLb5VuTZI_6DAAKvDWsbA60wUBYsk4WBsiG_AQADAgADeAADOAQ",
            "Orange", "Persian", 10, "Mischievous cat with shiny orange color"));
    cats.add(
        new CatSample("Parsa", "AgACAgQAAxkBAAIBO2mF4iPSibwDWARcaQ3lUOKrM6JNAAKwDWsbA60wUPmfyngt4OoyAQADAgADeQADOAQ",
            "White & Gray", "British", 15, "Calm cat with bright green eyes"));
    cats.add(
        new CatSample("Yalda", "AgACAgQAAxkBAAIBPmmF4j6wZGZToXhy7ZTpqZqXw7gpAAKxDWsbA60wUNnd9LgiCe4XAQADAgADeAADOAQ",
            "Spotted", "Mixed", 9, "Smart and curious cat with beautiful spots"));
    return cats;
  }

  private List<CatSample> getSampleCatsRu() {
    List<CatSample> cats = new ArrayList<>();
    cats.add(
        new CatSample("Снежок", "AgACAgQAAxkBAAIBKGmF4IoouqCLxqYndAAB_DnL9gK8gwACqg1rGwOtMFB5GlSH3Vy-CAEAAwIAA20AAzgE",
            "Белый", "Персидский", 12, "Очень милый и игривый котёнок с голубыми глазами, любит играть с мячиком"));
    cats.add(
        new CatSample("Тигр", "AgACAgQAAxkBAAIBKmmF4M10GCQONvA7gjOaKCa5jKhPAAKrDWsbA60wUH_xNATbI4aKAQADAgADbQADOAQ",
            "Оранжевый", "Микс", 8, "Энергичный и милый котёнок, всегда исследует новую среду"));
    cats.add(
        new CatSample("Ашкан", "AgACAgQAAxkBAAIBEWmF4CIBPeRaIz55cG_1mrEwGUWoAAKoDWsbA60wUDrGnal3BW73AQADAgADbQADOAQ",
            "Серый", "Британец", 24, "Спокойный и дружелюбный кот, любит спать у окна и смотреть на птиц"));
    cats.add(
        new CatSample("Лила", "AgACAgQAAxkBAAIBMmmF4Xb7AW_lcj98Q8cB8BKnZ0HtAAKtDWsbA60wUN9HXPXJGW2KAQADAgADbQADOAQ",
            "Чёрный", "Сиамский", 6, "Маленький игривый котёнок с яркими голубыми глазами"));
    cats.add(
        new CatSample("Бабак", "AgACAgQAAxkBAAIBNWmF4Y-KYabARQ5DxDWd2uNtJuScAAKuDWsbA60wUFs0zrujt6-aAQADAgADbQADOAQ",
            "Жёлтый", "Микс", 18, "Забавный и милый котёнок, который любит рыбу"));
    cats.add(
        new CatSample("Нико", "AgACAgQAAxkBAAIBOGmF4f9OQDX7KtH7dLb5VuTZI_6DAAKvDWsbA60wUBYsk4WBsiG_AQADAgADeAADOAQ",
            "Оранжевый", "Персидский", 10, "Озорной котёнок с ярким оранжевым окрасом"));
    cats.add(
        new CatSample("Парса", "AgACAgQAAxkBAAIBO2mF4iPSibwDWARcaQ3lUOKrM6JNAAKwDWsbA60wUPmfyngt4OoyAQADAgADeQADOAQ",
            "Бело-серый", "Британец", 15, "Спокойный кот с яркими зелёными глазами"));
    cats.add(
        new CatSample("Ялда", "AgACAgQAAxkBAAIBPmmF4j6wZGZToXhy7ZTpqZqXw7gpAAKxDWsbA60wUNnd9LgiCe4XAQADAgADeAADOAQ",
            "Пятнистый", "Микс", 9, "Умный и любопытный котёнок с красивыми пятнами"));
    return cats;
  }
}
