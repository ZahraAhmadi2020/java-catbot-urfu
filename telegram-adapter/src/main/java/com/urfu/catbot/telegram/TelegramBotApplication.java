package com.urfu.catbot.telegram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import com.urfu.catbot.telegram.bot.SimpleTelegramBot;

@SpringBootApplication
public class TelegramBotApplication {

  public static void main(String[] args) {
    SpringApplication.run(TelegramBotApplication.class, args);
    System.out.println("Telegram Adapter started successfully! ");
    System.out.println("  Bot: @zhra002021bot  ");
  }

  @Bean
  public TelegramBotsApi telegramBotsApi(SimpleTelegramBot bot) throws TelegramApiException {
    TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
    botsApi.registerBot(bot);
    System.out.println("🔌 Bot registered with Telegram API");
    return botsApi;
  }
}
