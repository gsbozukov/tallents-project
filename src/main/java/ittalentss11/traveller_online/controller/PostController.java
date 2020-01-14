package ittalentss11.traveller_online.controller;
import ittalentss11.traveller_online.controller.controller_exceptions.*;
import ittalentss11.traveller_online.model.dao.CategoryDAO;
import ittalentss11.traveller_online.model.dao.PostDAO;
import ittalentss11.traveller_online.model.dao.UserDAO;
import ittalentss11.traveller_online.model.dao.PostPictureDao;
import ittalentss11.traveller_online.model.dto.*;
import ittalentss11.traveller_online.model.pojo.Category;
import ittalentss11.traveller_online.model.pojo.Post;
import ittalentss11.traveller_online.model.pojo.PostPicture;
import ittalentss11.traveller_online.model.pojo.User;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;
import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;

@RestController
public class PostController {
    @Autowired
    private PostDAO postDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private CategoryDAO categoryDAO;
    @Autowired
    private PostPictureDao postPictureDao;
    private static final int MAX_PICTURES = 3;

    //============ ADD A POST ==================//
    @SneakyThrows
    @PostMapping("/posts")
    public PostDTO addPost(@Valid @RequestBody PostDTO postDTO, HttpSession session) {
        //Is the user logged in?
        User u = (User) session.getAttribute(UserController.USER_LOGGED);
        if (u == null) {
            throw new AuthorizationException();
        }
        Post post = new Post();
        post.setUser(u);
        if (postDTO.getMapUrl() == null || postDTO.getMapUrl().isEmpty()){
            throw new BadRequestException("Make sure to fill your map URL.");
        }
        post.setMapUrl(postDTO.getMapUrl());
        if (postDTO.getLocationName() == null || postDTO.getLocationName().isEmpty()){
            throw new BadRequestException("Make sure to fill your location name.");
        }
        post.setLocationName(postDTO.getLocationName());
        if (postDTO.getDescription() == null || postDTO.getDescription().isEmpty()){
            throw new BadRequestException("Make sure to add a short description of your trip.");
        }
        post.setDescription(postDTO.getDescription());
        post.setOtherInfo(postDTO.getOtherInfo());
        //validate if there is a category with id
        Category category = categoryDAO.getCategoryById(postDTO.getCategoryId());
        post.setCategory(category);
        if (!postDTO.checkCoordinates(postDTO.getCoordinates())) {
                throw new WrongCoordinatesException();
        }
        post.setCoordinates(postDTO.getCoordinates());
        post.setDateTime(LocalDateTime.now());
        postDAO.addPost(post);
        return new PostDTO(post);
    }
    //============VIEW POST BY ID ==================//
    @SneakyThrows
    @GetMapping("/posts/{id}")
    public ViewPostDTO viewPost (@PathVariable("id") long id){
        Post post = postDAO.getPostById(id);
        return new ViewPostDTO(post, postPictureDao.getPicturesByPostId(post.getId()));
    }
    //============ ADD PICTURE TO POST ==================//
    @SneakyThrows
    @PostMapping("/posts/{id}/pictures")
    public PictureDTO addPicture(@RequestPart(value = "picture") MultipartFile multipartFile, @PathVariable("id") long id,
                             HttpSession session){
        //first we check if the user is logged in
        User user = (User) session.getAttribute(UserController.USER_LOGGED);
        if (user == null){
            throw new AuthorizationException();
        }
        // we check if there is a post with id
        Post post = postDAO.getPostById(id);
        if (postPictureDao.getNumberOfPictures(post.getId()) >= MAX_PICTURES){
            throw new PostPicturePerPostException();
        }
        if (!post.getUser().getUsername().equals(user.getUsername())){
            throw new BadRequestException("You cannot upload a picture on someone else's post.");
        }
        String path = "C://posts//png//";
        String pictureName = PostController.getNameForUpload(multipartFile.getOriginalFilename(), user, post);
        File picture = new File(path + pictureName);
        FileOutputStream fos = new FileOutputStream(picture);
        fos.write(multipartFile.getBytes());
        fos.close();
        String mimeType = new MimetypesFileTypeMap().getContentType(picture);
        if(!mimeType.substring(0, 5).equalsIgnoreCase("image")){
            picture.delete();
            throw new BadRequestException("File format not supported, please upload pictures only");
        }
        PostPicture postPicture  = postPictureDao.addPostPicture(post, pictureName);
        return new PictureDTO(postPicture);
    }
    //============ ADD VIDEO TO POST ==================//
    @SneakyThrows
    @PostMapping("/posts/{id}/videos")
    public ViewPostDTO addVideos(@RequestPart(value = "videos") MultipartFile multipartFile, @PathVariable("id") long id,
                                 HttpSession session){
        //first we check if the use is logged
        User user = (User) session.getAttribute(UserController.USER_LOGGED);
        if (user == null){
            throw new AuthorizationException();
        }
        // we check if there is a post with id
        Post post = postDAO.getPostById(id);
        if (!post.getUser().getUsername().equals(user.getUsername())){
            throw new BadRequestException("You cannot modify posts that are not yours.");
        }
        String path = "C://posts//videos//";
        String videosName = PostController.getNameForUpload(multipartFile.getOriginalFilename(), user, post);
        if (post.getVideoUrl() != null || !post.getVideoUrl().isEmpty()){
            //if a user has a video on post, we delete the old one from path folder
            File file = new File(path + post.getVideoUrl());
            file.delete();
        }
        File videos = new File(path + videosName);
        FileOutputStream fos = new FileOutputStream(videos);
        fos.write(multipartFile.getBytes());
        fos.close();
        String mimeType = new MimetypesFileTypeMap().getContentType(videos);
        if(!mimeType.substring(0, 6).contains("video")){
            videos.delete();
            throw new BadRequestException("File format not supported, please upload videos only");
        }
        post.setVideoUrl(videosName);
        postDAO.addVideos(post, videosName);
        return new ViewPostDTO(post);
    }
    public static String getNameForUpload(String name, User user, Post post){
        //name should have one . in it
        String[] all = name.split("\\.", 2);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd-hh-mm-ss");
        LocalDateTime localDateTime = LocalDateTime.now();
        String parse = localDateTime.format(dateTimeFormatter);
        String nameWithoutId = all[0];
        String formatForPicture = all[1];
        String nameWithId = nameWithoutId + "_" + parse + "_" + user.getId() + "_" + post.getId() +  "." + formatForPicture;
        System.out.println(name);
        return nameWithId;
    }
    //============ TAG ANOTHER USER ==================//
    @SneakyThrows
    @GetMapping("/posts/{pId}/users/{uId}")
    public TagDTO tagSomeone(@PathVariable("pId") Long pId, @PathVariable("uId") Long uId, HttpSession session){
        //Is the user logged in?
        User u = (User) session.getAttribute(UserController.USER_LOGGED);
        if (u == null){
            throw new AuthorizationException();
        }
        //Check if user is trying to tag someone on his post
        Post post = postDAO.getPostById(pId);
        if (post.getUser().getId()!=u.getId()){
            throw new AuthorizationException("You cannot tag people on someone else's post");
        }
        User taggedUser = userDAO.getUserById(uId);
        post.addTaggedUser(taggedUser);
        postDAO.save(post);
        return new TagDTO(uId, pId);
    }

    //Doesnt need to be logged in
    @SneakyThrows
    @GetMapping("/posts/byDate/{dateTime}")
    public ArrayList<ViewPostsAndLikesDTO> sortByDateAndLikes(@PathVariable("dateTime") String date){
        if(date == null){
            throw new BadRequestException("Please enter a valid date.");
        }
        //will throw exception if date is not valid
        LocalDate.parse(date);
        return postDAO.getPostsSortedByDateAndLikes(date);
    }
    @SneakyThrows
    @GetMapping("/postsByUname/{user_name}")
    public ArrayList<ViewPostDTO> getPostByUsername(@PathVariable("user_name") String username){
        return postDAO.getPostsByUsername(username);
    }
    @SneakyThrows
    @GetMapping("/postsByTag/{user_id}")
    public ArrayList<ViewPostDTO> getPostByTag(@PathVariable("user_id") int id){
        return postDAO.getPostsByTag(id);
    }

    @SneakyThrows
    @RequestMapping("/posts/{id}")
    public RedirectView viewMap(@PathVariable("id") Long id) {
        RedirectView redirectView = new RedirectView();
        Post post = postDAO.getPostById(id);
        //TODO : Validation of external link?
        String link = post.getMapUrl();
        redirectView.setUrl(link);
        return redirectView;
    }
}
