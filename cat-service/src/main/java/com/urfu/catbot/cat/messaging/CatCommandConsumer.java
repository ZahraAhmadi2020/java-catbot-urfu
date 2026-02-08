package com.urfu.catbot.cat.messaging;

import com.urfu.catbot.cat.service.CatService;
import com.urfu.catbot.shared.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CatCommandConsumer {

  private final CatService catService;

  @RabbitListener(queues = "cat.commands")
  public void handleAddCat(AddCatCommand command) {
    catService.addCat(
        command.getName(),
        command.getPhotoFileId(),
        command.getUserId(),
        command.getUsername(),
        command.getColor(),
        command.getBreed(),
        command.getAgeMonths(),
        command.getDescription());
  }

  @RabbitListener(queues = "cat.commands")
  public void handleAddReaction(AddReactionCommand command) {
    catService.addReaction(
        command.getCatId(),
        command.getUserId(),
        command.getUsername(),
        command.getEmoji());
  }

  @RabbitListener(queues = "cat.commands")
  public void handleAddComment(AddCommentCommand command) {
    catService.addComment(
        command.getCatId(),
        command.getUserId(),
        command.getUsername(),
        command.getText());
  }

  @RabbitListener(queues = "cat.commands")
  public void handleDeleteCat(DeleteCatCommand command) {
    catService.deleteCat(command.getCatId(), command.getUserId());
  }
}
