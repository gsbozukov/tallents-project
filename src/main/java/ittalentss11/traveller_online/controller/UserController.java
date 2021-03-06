package ittalentss11.traveller_online.controller;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import ittalentss11.traveller_online.controller.controller_exceptions.*;
import ittalentss11.traveller_online.model.dao.UserDAO;
import ittalentss11.traveller_online.model.dto.*;
import ittalentss11.traveller_online.model.pojo.*;
import ittalentss11.traveller_online.model.repository_ORM.UserRepository;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

@RestController
public class UserController {

    public static final String USER_LOGGED = "logged";
    @Autowired
    private UserDAO userDao;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LoginVerificationController loginVerification;
    private Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
    //USER REGISTRATION
    @SneakyThrows
    @PostMapping(value = "/users")
    public UserNoSensitiveDTO add (@RequestBody UserRegDTO user, HttpSession session){
        //VERIFICATIONS:
        //Check if username is available
        if (userRepository.countUsersByUsernameEquals(user.getUsername())>=1){
            throw new UsernameTakenException("This username already exists, please pick another one.");
        }
        //Check if email is not used already
        if (userRepository.countUsersByEmailEquals(user.getEmail())>=1){
            throw new EmailTakenException("Email already exists, please pick another one.");
        }
        //Check if passwords match
        if (!user.getPassword().equals(user.getConfPassword())){
            throw new NoPassMatchException("Wrong password setup, please make sure to confirm your password.");
        }
        if (!user.checkPasswordPatterns(user.getPassword())){
            throw new RegisterCheckException
                    ("Please enter a password containing at least 6 letters or numbers without spaces." +
                            "Your password can contain dots, dashes or underscores");
        }
        //Verify email validity
        if (!user.checkEmail(user.getEmail())){
            throw new RegisterCheckException("You have entered an invalid email.");
        }
        //Verify username and password
        if (!user.checkUsernameAndPass(user.getUsername(), user.getPassword())){
            throw new RegisterCheckException("Make sure that your username/password " +
                    "contains (3-40 characters) alphanumericals, dots, dashes or underscores");
        }
        //Verify first and last names
        if (!user.firstAndLastNames(user.getFirstName(), user.getLastName())){
            throw new RegisterCheckException("Your first and last names must " +
                    "contain only alphabetical characters (2-40 chars)");
        }
        //Create user and return UserDTO as confirmation
        User created = new User(user);
        userDao.register(created);
        session.setAttribute(USER_LOGGED, created);
        UserNoSensitiveDTO userNoSensitiveDTO = new UserNoSensitiveDTO(created);
        return userNoSensitiveDTO;
    }
    @SneakyThrows
    @PostMapping(value = "/users/login")
    public UserNoSensitiveDTO login(@RequestBody UserLoginDTO userLoginDTO, HttpSession session){
        //Check if username exists
        User u = null;
        if (!userDao.foundUsernameForLogin(userLoginDTO.getUsername())){
            throw new AuthorizationException("Wrong credentials, please verify your username and password.");
        }
        //If it does check for username/password match with argon2
        u = userDao.getUserByUsername(userLoginDTO.getUsername());
        if (!argon2.verify(u.getPassword(), userLoginDTO.getPassword())){
            throw new AuthorizationException("Wrong credentials, please verify your username and password.");
        }
        session.setAttribute(USER_LOGGED, u);
        return new UserNoSensitiveDTO(u);
    }
    @SneakyThrows
    @GetMapping(value = "/follow/{id}")
    public UserNoSensitiveDTO followUser (@PathVariable("id") Long id, HttpSession session){
        User u = loginVerification.checkIfLoggedIn(session);
        //Getting and verifying user ID
        User followUser;
        Optional<User> optionalFollowed = userRepository.findById(id);
        if (optionalFollowed.isPresent()){
            followUser = optionalFollowed.get();
        }
        else {
            throw new BadRequestException("Sorry, this user does not exist.");
        }
        if (followUser.getId() == u.getId()){
            throw new BadRequestException("You cannot follow yourself.");
        }
        followUser.addFollower(u);
        userRepository.save(followUser);
        return new UserNoSensitiveDTO
                (followUser);
    }
    @GetMapping("/users/newsFeed/")
    @SneakyThrows
    public HashMap<String, ArrayList<ViewPostDTO>> getTags(HttpSession session){
        User user = loginVerification.checkIfLoggedIn(session);
        HashMap<String, ArrayList<ViewPostDTO>> arr = userDao.getNewsFeed(user);
        if (arr.isEmpty()){
            return userDao.getNewsFeedNewUser();
        }
        return arr;
    }
    @SneakyThrows
    @GetMapping(value = "/unfollow/{id}")
    public UserNoSensitiveDTO unfollowUser (@PathVariable("id") Long id, HttpSession session){
        //Is the user logged in?
        User u = loginVerification.checkIfLoggedIn(session);
        //Getting and verifying user ID
        User unfollowUser;
        Optional<User> optionalFollowed = userRepository.findById(id);
        if (optionalFollowed.isPresent()){
            unfollowUser = optionalFollowed.get();
        }
        else {
            throw new BadRequestException("Sorry, this user does not exist.");
        }
        if (!unfollowUser.hasFollower(u)){
            throw new BadRequestException("Sorry, you cannot unfollow an user you are not following.");
        }
        unfollowUser.removeFollower(u);
        userRepository.save(unfollowUser);
        return new UserNoSensitiveDTO(unfollowUser);
    }

    @GetMapping("/users/logout")
    public void logOut(HttpSession session){
        session.invalidate();
    }
}
