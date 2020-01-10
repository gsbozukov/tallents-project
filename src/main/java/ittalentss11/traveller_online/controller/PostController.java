package ittalentss11.traveller_online.controller;
import ittalentss11.traveller_online.controller.controller_exceptions.*;
import ittalentss11.traveller_online.model.dao.CategoryDAO;
import ittalentss11.traveller_online.model.dao.PostDAO;
import ittalentss11.traveller_online.model.dao.PostPictureDao;

import ittalentss11.traveller_online.model.dto.PostDTO;
import ittalentss11.traveller_online.model.pojo.Category;
import ittalentss11.traveller_online.model.pojo.Post;
import ittalentss11.traveller_online.model.pojo.PostPicture;
import ittalentss11.traveller_online.model.pojo.User;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
public class PostController {
    @Autowired
    private PostDAO postDAO;
    @Autowired
    private CategoryDAO categoryDAO;
    @Autowired
    private PostPictureDao postPictureDao;
    private static final int MAX_PICTURES = 3;
    //Posting a post:
    @SneakyThrows
    @PostMapping("/posts")
    public String addPost(@RequestBody PostDTO postDTO, HttpSession session) {
        //Is the user logged in?
        User u = (User) session.getAttribute(UserController.USER_LOGGED);
        if (u == null) {
            throw new AuthorizationException();
        }
        Post post = new Post();
        //Adding to post: user, description of pictures, video url if any, info for video, categoryID, coordinates
        //mapUrl and location name
        post.setUser(u);
        System.out.println(post.getUser().getId());
        post.setDescription(postDTO.getDescription());
        post.setVideoUrl(postDTO.getVideoUrl());
        post.setOtherInfo(postDTO.getOtherInfo());
        //validate if there is a category with id
        Category category = categoryDAO.getByName(postDTO.getCategoryName());
        if (category == null) {
            throw new MissingCategoryException();
        }
        post.setCategory(category);
        if (postDTO.checkCoordinates(postDTO.getCoordinates()) == false) {
                throw new WrongCoordinatesException();
        }
        post.setCoordinates(postDTO.getCoordinates());
        post.setMapUrl(postDTO.getMapUrl());
        post.setLocationName(postDTO.getLocationName());
        postDAO.addPost(post);
        //return post;
        //CHANGED THE RETURN TYPE TO STRING BECAUSE WE'RE RETURNING THE CRYPTED PASSWORD when returning
        //THE USER JSON
        return "Your post was successfully added!";
    }
    @SneakyThrows
    @PostMapping("/posts/{id}/upload")
    public String addPicture(@RequestPart(value = "picture") MultipartFile multipartFile, @PathVariable("id") long id,
                             HttpSession session){
        //first we check if the use is logged
        User user = (User) session.getAttribute(UserController.USER_LOGGED);
        if (user == null){
            throw new AuthorizationException();
        }
        // we check if there is a post with id
        Post post = postDAO.getPostById(id);
        if (post == null){
            throw new BadRequestException();
        }
        if (postPictureDao.getAllPictures((int) post.getId()) > MAX_PICTURES){
            throw new PostPicturePerPostException();
        }
        if (post.getUser() != user){
            throw new AuthorizationException();
        }
        String namesWhole = multipartFile.getOriginalFilename();
        String path = "C://posts//png//";
        String[] all = namesWhole.split("\\.", 2);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-mm-dd-hh-mm-ss");
        LocalDateTime localDateTime = LocalDateTime.now();
        String parse = localDateTime.format(dateTimeFormatter);
        String nameWithId = all[0] + "_" + parse + "_" + user.getId() + "." + all[1];
        System.out.println(namesWhole);
        File picture = new File(path + nameWithId);
        FileOutputStream fos = new FileOutputStream(picture);
        fos.write(multipartFile.getBytes());
        fos.close();
        PostPicture postPicture = new PostPicture();
        postPictureDao.addPostPicture(post, nameWithId);
        postPicture.setPost(post);
        postPicture.setPictureUrl(nameWithId);
        //check if there are no more than 3 pictures
        //check if the file is a picture
        // create a folder with the same name d:/tallents/png
        //name the file picture_url_id.png
        //move to folder
        //write in posts_pictures post_id and picture url
        return picture.getName();
    }
}
