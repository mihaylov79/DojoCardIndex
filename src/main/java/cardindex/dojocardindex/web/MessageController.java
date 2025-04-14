package cardindex.dojocardindex.web;


import cardindex.dojocardindex.Message.Service.MessageService;
import cardindex.dojocardindex.Message.models.Message;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import cardindex.dojocardindex.web.dto.MessageReplyRequest;
import cardindex.dojocardindex.web.dto.SendMessageRequest;
import cardindex.dojocardindex.web.dto.ShortMessageRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

@Controller
public class MessageController {

    private final UserService userService;
    private final MessageService messageService;

    public MessageController(UserService userService, MessageService messageService) {
        this.userService = userService;
        this.messageService = messageService;
    }

    @GetMapping("/messages/send")
    public ModelAndView getSendMessagePage (@AuthenticationPrincipal CustomUserDetails details){

        User user = userService.getUserById(details.getId());
        List<User>recipients = userService.getRecipients(details.getId());

        ModelAndView modelAndView = new ModelAndView("new-message");
        modelAndView.addObject("user",user);
        modelAndView.addObject("recipients",recipients);
        modelAndView.addObject("sendMessageRequest", new SendMessageRequest());

        return modelAndView;
    }

    @PostMapping("messages/send")
    public ModelAndView sendMessage(@AuthenticationPrincipal CustomUserDetails details, @Valid SendMessageRequest sendMessageRequest, BindingResult result){

        User user = userService.getUserById(details.getId());
        List<User>recipients = userService.getRecipients(details.getId());

        if (result.hasErrors()){
            ModelAndView modelAndView = new ModelAndView("new-message");
            modelAndView.addObject("user",user);
            modelAndView.addObject("recipients",recipients);
            return modelAndView;
        }

        messageService.sendMessage(sendMessageRequest);

        return new ModelAndView("redirect:/home");

    }
    @GetMapping("/messages/reply/{id}")
    public ModelAndView getReplyPage(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails details){

        User user = userService.getUserById(details.getId());

        Message originalMessage = messageService.findMessageById(id);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("message-reply");
        modelAndView.addObject("user",user);
        modelAndView.addObject("message", originalMessage);
        modelAndView.addObject("messageReplyRequest", new MessageReplyRequest());

        return modelAndView;

    }
    @PostMapping("/messages/reply/{messageId}")
    public ModelAndView replyMessage(@PathVariable UUID messageId,
                                     @ModelAttribute @Valid MessageReplyRequest messageReplyRequest,
                                     BindingResult result,
                                     @AuthenticationPrincipal CustomUserDetails details){

        User currentUser = userService.getUserById(details.getId());

        if (result.hasErrors()){
            ModelAndView modelAndView = new ModelAndView("message-reply");
            modelAndView.addObject("messageReplyRequest",messageReplyRequest);
            modelAndView.addObject("message",messageService.findMessageById(messageId));
            modelAndView.addObject("currentUser",currentUser);
            return modelAndView;
        }

        messageService.replyMessage(messageId, messageReplyRequest.getMessageContent());

        return new ModelAndView("redirect:/home");

    }

    @GetMapping("/messages/send/{userId}")
    public ModelAndView getSendMessageToUserIdPage(@PathVariable UUID userId,
                                                   @AuthenticationPrincipal CustomUserDetails details) {

        User currentUser = userService.getUserById(details.getId());
        User recipient = userService.getUserById(userId);

        ModelAndView modelAndView = new ModelAndView("send-message-toId");
        modelAndView.addObject("recipient", recipient);
        modelAndView.addObject("currentUser", currentUser);
        modelAndView.addObject("messageRequest", new ShortMessageRequest()); // 🔹 Добавяме празен DTO

        return modelAndView;
    }

    @PostMapping("/messages/send/{userId}")
    public ModelAndView sendMessageToUserId(@PathVariable UUID userId,
                                            @Valid @ModelAttribute("messageRequest") ShortMessageRequest messageRequest,
                                            BindingResult result,
                                            @AuthenticationPrincipal CustomUserDetails details){

        if (result.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("send-message-toId");
            modelAndView.addObject("messageRequest",messageRequest);
            modelAndView.addObject("currentUser", userService.getUserById(details.getId()));
            modelAndView.addObject("recipient", userService.getUserById(userId));
            return modelAndView;
        }

        messageService.sendMessageToUserId(userId,messageRequest.getMessageContent());

        return new ModelAndView("redirect:/users/list");
    }

    @GetMapping("/messages/remove/{id}")
    public ModelAndView removeReceivedMessage(@PathVariable UUID id,
                                      @AuthenticationPrincipal CustomUserDetails details){
        User user = userService.getUserById(details.getId());

        messageService.removeMessage(id);

        ModelAndView modelAndView = new ModelAndView("redirect:/home");
        modelAndView.addObject("user", user);

        return modelAndView;
    }
}
