# Доклад за използване на HTML шаблони

## Анализ на използването на templates (02.02.2026)

### ✅ ИЗПОЛЗВАНИ ШАБЛОНИ (35 броя):

1. **index.html** - IndexController.getIndexPage()
2. **register.html** - IndexController.getRegistrationPage()
3. **login.html** - IndexController.getLoginPage()
4. **home-fixed.html** - IndexController.getHomePage()
5. **monitoring.html** - IndexController.getStatusPage() - вече ИЗПОЛЗВА СЕ!
6. **events.html** - EventController.getEventsPage()
7. **new-event.html** - EventController.getCreateEventPage()
8. **edit-event.html** - EventController.getEditEventPage()
9. **set-winners.html** - EventController.showSetWinnersPage()
10. **event-details-universal.html** - EventController.getEventDetails()
11. **users-list.html** - UserController.getActiveUsersPage()
12. **user-details-test.html** - UserController.getUserDetailsPage()
13. **EditUserProfileUser.html** - UserController.getUserEditPage()
14. **password-change.html** - UserController.getPassWordChangePage()
15. **active-users.html** - UserController.getActiveUsersListPag()
16. **addUser.html** - UserController.getAddNewUserPage()
17. **register-requests.html** - UserController.getRegisterRequests()
18. **all-users-list.html** - UserController.getAllUsersPage()
19. **admin-user-edit.html** - UserController.getEditUserDetailsByAdminPage()
20. **pending-requests.html** - EventParticipationRequestController.getPendingRequests()
21. **request-reject.html** - EventParticipationRequestController.getRejectRequestPage()
22. **user-partisipation-requests.html** - EventParticipationRequestController.getUserRequestsPage()
23. **not-pending.html** - EventParticipationRequestController.getNotPendingRequestsPage()
24. **posts.html** - PostController.getPostPage()
25. **post-request.html** - PostController.getCreatePostPage()
26. **new-message.html** - MessageController.getSendMessagePage()
27. **message-reply.html** - MessageController.getReplyPage()
28. **send-message-toId.html** - MessageController.getSendMessageToUserIdPage()
29. **notifications.html** - NotificationController.getNotificationsPage()
30. **new-comment.html** - CommentController.getNewCommentPage() - вече ИЗПОЛЗВА СЕ!
31. **forgotten-password-form.html** - ForgottenPasswordTokenController.getForgottenPasswordPage()
32. **forgotten-password-success.html** - ForgottenPasswordTokenController.submitForgottenPasswordRequest()
33. **reset-password-form.html** - ForgottenPasswordTokenController.getResetPasswordPage()
34. **reset-password-success.html** - ForgottenPasswordTokenController.resetPassword()
35. **bad-request.html** - ExceptionAdvice (error handler)
36. **not-found.html** - ExceptionAdvice (error handler)
37. **internal-server-error.html** - ExceptionAdvice (error handler)

### ❌ НЕИЗПОЛЗВАНИ ШАБЛОНИ - БЕЗОПАСНО ЗА ИЗТРИВАНЕ (5 броя):

1. ❌ **event-details.html** - закоментиран метод в EventParticipationRequestController (ред 91-103)
   - Заменен с event-details-universal.html
   
2. ❌ **user.html** - НЯМА контролер метод, който го използва
   - Стар user profile template

3. ❌ **send-message.html** - НЯМА контролер метод, който го използва  
   - Заменен с new-message.html и send-message-toId.html

4. ❌ **event-details-export.html** - закоментиран метод в EventController (ред 215-231)
   - Може да се изтрие САМО ако методът няма да се активира

5. ❌ **user-details.html** - НЯМА контролер метод, който го използва
   - Заменен с user-details-test.html

### ⚠️ СЪМНИТЕЛЕН ФАЙЛ:

- **monitoring2.html** - НЯМА контролер метод
  - monitoring.html СЕ ИЗПОЛЗВА от IndexController.getStatusPage()
  - monitoring2.html изглежда като дубликат или стара версия

### 📁 ДИРЕКТОРИИ:

- **fragments/** - използва се за nav фрагменти (nav.html)

### 📝 ДРУГИ ФАЙЛОВЕ:

- **ages.md** - Markdown файл, вероятно за документация или бележки

## ✅ ПОТВЪРДЕНИ ЗА ИЗТРИВАНЕ:

1. ✅ **event-details.html** - закоментиран ендпойнт
2. ✅ **user.html** - не се използва
3. ✅ **send-message.html** - не се използва
4. ⚠️ **event-details-export.html** - закоментиран метод (решете дали ще го активирате)
5. ✅ **user-details.html** - заменен с user-details-test.html
6. ⚠️ **monitoring2.html** - вероятно дубликат на monitoring.html

## 📝 БЕЛЕЖКИ:

- **event-details.html** вече е закоментиран в EventParticipationRequestController с пълно обяснение (ред 89-103)
- **CommentController** съществува и **new-comment.html** СЕ ИЗПОЛЗВА активно
- **monitoring.html** СЕ ИЗПОЛЗВА от `/status` ендпойнта (само за ADMIN)
- Всички password reset шаблони се използват от ForgottenPasswordTokenController


