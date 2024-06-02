package com.becoder.controller;

import com.becoder.model.Document;
import com.becoder.repository.DocumentRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Controller
public class DocumentController {

    @Autowired
    private DocumentRepository documentRepo;

    @GetMapping("/search")
    public String searchDocuments(@RequestParam("query") String query, Model model) {
        List<Document> documents = documentRepo.searchDocuments(query);
        model.addAttribute("documents", documents);
        return "documents"; // Assuming you have a template named 'documents.html'
    }

    @GetMapping("/")
    public String index(Model m) {
        List<Document> documents = documentRepo.findAll();
        m.addAttribute("documents", documents);
        return "index";
    }

    @PostMapping("/documentUpload")
    public String documentUpload(@RequestParam("doc") MultipartFile file,
                                 @RequestParam("author") String author,
                                 @RequestParam("description") String description,
                                 @RequestParam("category") String category,
                                 @RequestParam("creationDate") String creationDate,
                                 HttpSession session) {

        try {
            String filename = file.getOriginalFilename();
            String uploadDir = System.getProperty("user.dir") + "/uploads/documents";
            File saveDir = new File(uploadDir);
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }
            Path path = Paths.get(uploadDir, filename);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            Document document = new Document();
            document.setDocumentName(filename);
            document.setPath(path.toString());
            document.setAuthor(author);
            document.setDescription(description);
            document.setCategory(category);
            document.setCreationDate(creationDate);

            documentRepo.save(document);

            session.setAttribute("msg", "Документ загружен успешно");

        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("msg", "Ошибка при загрузке документа");
        }

        return "redirect:/";
    }

    @GetMapping("/documents")
    public String listFiles(@RequestParam(value = "category", required = false) String category, Model model) {
        List<Document> documents;
        if (category != null && !category.isEmpty()) {
            documents = documentRepo.findByCategory(category);
        } else {
            documents = documentRepo.findAll();
        }
        model.addAttribute("documents", documents);
        return "documents";
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Integer id) {
        try {
            Document document = documentRepo.findById(id).orElseThrow(() -> new RuntimeException("File not found"));
            Path filePath = Paths.get(document.getPath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("Could not read the file!");
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (IOException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> previewFile(@PathVariable Integer id) {
        try {
            Document document = documentRepo.findById(id).orElseThrow(() -> new RuntimeException("File not found"));
            Path filePath = Paths.get(document.getPath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("Could not read the file!");
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + UriUtils.encode(resource.getFilename(), StandardCharsets.UTF_8))
                    .body(resource);

        } catch (IOException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}
