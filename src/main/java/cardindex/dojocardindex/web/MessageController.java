package cardindex.dojocardindex.web;


import cardindex.dojocardindex.Message.Service.MessageService;
import cardindex.dojocardindex.Message.models.Message;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import cardindex.dojocardindex.web.dto.SendMessageRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

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

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("send-message");
        modelAndView.addObject("user",user);
        modelAndView.addObject("sendMessageRequest", new SendMessageRequest());

        return modelAndView;
    }

    @PostMapping("messages/send")
    public ModelAndView sendMessage(@AuthenticationPrincipal CustomUserDetails details, @Valid SendMessageRequest sendMessageRequest, BindingResult result){

        User user = userService.getUserById(details.getId());

        if (result.hasErrors()){
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("send-message");
            modelAndView.addObject("user",user);
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

        return modelAndView;

    }
    @PostMapping("/messages/reply")
    public ModelAndView replyMessage(@RequestParam UUID responseToMessageId,
                                     @RequestParam String content,
                                     @AuthenticationPrincipal CustomUserDetails details){

        User user = userService.getUserById(details.getId());

        messageService.replyMessage(responseToMessageId, content);

        ModelAndView modelAndView = new ModelAndView("redirect:/home");
        modelAndView.addObject(user);

        return modelAndView;

    }

    @GetMapping("/messages/send/{userId}")
    public ModelAndView getSendMessageToUserIdPage(@PathVariable UUID userId,@AuthenticationPrincipal CustomUserDetails details){

        User currentUser = userService.getUserById(details.getId());
        User recipient = userService.getUserById(userId);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("send-message-toId");
        modelAndView.addObject("recipient",recipient);
        modelAndView.addObject("currentUser",currentUser);
        return modelAndView;
    }

    @PostMapping("/messages/send/{userId}")
    public ModelAndView sendMessageToUserId(@PathVariable UUID userId,@RequestParam String messageContent,@AuthenticationPrincipal CustomUserDetails details){

        messageService.sendMessageToUserId(userId,messageContent);

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
