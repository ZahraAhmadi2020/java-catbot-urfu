package com.urfu.catbot.telegram.messaging;

import com.urfu.catbot.shared.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CatCommandProducer {

  private final RabbitTemplate rabbitTemplate;

  public void sendAddCat(AddCatCommand command) {
    rabbitTemplate.convertAndSend("cat.exchange", "cat.command", command);
  }

  public void sendAddReaction(AddReactionCommand command) {
    rabbitTemplate.convertAndSend("cat.exchange", "cat.command", command);
  }

  public void sendAddComment(AddCommentCommand command) {
    rabbitTemplate.convertAndSend("cat.exchange", "cat.command", command);
  }

  public void sendDeleteCat(DeleteCatCommand command) {
    rabbitTemplate.convertAndSend("cat.exchange", "cat.command", command);
  }
}
