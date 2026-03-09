package cardindex.dojocardindex.web;

import cardindex.dojocardindex.Document.model.Document;
import cardindex.dojocardindex.Document.model.DocumentCategory;
import cardindex.dojocardindex.Document.service.DocumentService;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import cardindex.dojocardindex.web.dto.CreateDocumentRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final UserService userService;

    @Autowired
    public DocumentController(DocumentService documentService, UserService userService) {
        this.documentService = documentService;
        this.userService = userService;
    }

    @GetMapping
    public ModelAndView getDocumentsPage(@AuthenticationPrincipal CustomUserDetails details,
                                         @RequestParam(required = false) DocumentCategory category) {

        User currentUser = userService.getUserById(details.getId());
        List<Document> documents = documentService.getDocuments(category);

        ModelAndView modelAndView = new ModelAndView("documents");
        modelAndView.addObject("currentUser", currentUser);
        modelAndView.addObject("documents", documents);
        modelAndView.addObject("categories", DocumentCategory.values());
        modelAndView.addObject("selectedCategory", category);
        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @GetMapping("/upload")
    public ModelAndView getUploadPage(@AuthenticationPrincipal
                                      CustomUserDetails details){

        User currentUser = userService.getUserById(details.getId());

        ModelAndView modelAndView = new ModelAndView("upload-document");
        modelAndView.addObject("currentUser", currentUser);
        modelAndView.addObject("createDocumentRequest", new CreateDocumentRequest());
        modelAndView.addObject("categories", DocumentCategory.values());
        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @PostMapping("/upload")
    public ModelAndView uploadDocument(@AuthenticationPrincipal
                                       CustomUserDetails details,
                                       @Valid CreateDocumentRequest request,
                                       BindingResult result,
                                       @RequestParam("file")MultipartFile file,
                                       RedirectAttributes redirectAttributes){

        User currentUser = userService.getUserById(details.getId());

        if (result.hasErrors()){
            ModelAndView modelAndView = new ModelAndView("upload-document");
            modelAndView.addObject("currentUser", currentUser);
            modelAndView.addObject("categories", DocumentCategory.values());
            return modelAndView;
        }

        documentService.uploadDocument(request, file);

        redirectAttributes.addFlashAttribute("success",
                        "Документът е качен успешно!");

        return new ModelAndView("redirect:/documents");
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @PostMapping("/delete/{id}")
    public ModelAndView deleteDocument(@PathVariable UUID id,
                                       RedirectAttributes redirectAttributes) {

        documentService.deleteDocument(id);
        redirectAttributes.addFlashAttribute("success",
                "Документът беше изтрит успешно!");

        return new ModelAndView("redirect:/documents");

    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @PostMapping("/replace/{id}")
    public ModelAndView replaceDocument(@PathVariable UUID id,
                                        @RequestParam("file") MultipartFile file,
                                        RedirectAttributes redirectAttributes){

        documentService.replaceDocument(id,file);

        redirectAttributes.addFlashAttribute("success", "Документът беше заменен успешно!");

        return new ModelAndView("redirect:/documents");
    }

}
