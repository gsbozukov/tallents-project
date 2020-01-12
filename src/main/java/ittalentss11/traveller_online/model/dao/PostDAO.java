package ittalentss11.traveller_online.model.dao;
import ittalentss11.traveller_online.controller.controller_exceptions.BadRequestException;
import ittalentss11.traveller_online.model.dto.PostDTO;
import ittalentss11.traveller_online.model.dto.ViewPostDTO;
import ittalentss11.traveller_online.model.pojo.Post;
import ittalentss11.traveller_online.model.repository_ORM.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class PostDAO {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    PostRepository postRepository;

    private static final String INSERT_POST =
            "INSERT INTO final_project.posts " +
            "(user_id, video_url, description , other_info, category_id, coordinates, map_url, location_name) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
    public static final String UPDATE_POST_FOR_VIDEOS = "UPDATE final_project.posts SET video_url = ? WHERE id = ?;";
    public static final String GET_POSTS_BY_USERNAME = "SELECT p.* FROM final_project.posts JOIN users AS un ON p.user_id = un.id WHERE un.username LIKE CONCAT('%', ? ,'%');";
    //User post a post
    public void addPost(Post post) throws SQLException {
        Connection connection = jdbcTemplate.getDataSource().getConnection();
        try(PreparedStatement ps = connection.prepareStatement(INSERT_POST, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, (int) post.getUser().getId());
            ps.setString(2, post.getVideoUrl());
            ps.setString(3, post.getDescription());
            ps.setString(4, post.getOtherInfo());
            ps.setInt(5, (int) post.getCategory().getId());
            ps.setString(6, post.getCoordinates());
            ps.setString(7, post.getMapUrl());
            ps.setString(8, post.getLocationName());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            post.setId(keys.getLong(1));
        }
    }
    public void addVideos(Post post, String name) throws SQLException {
        Connection connection = jdbcTemplate.getDataSource().getConnection();
        try(PreparedStatement ps = connection.prepareStatement(UPDATE_POST_FOR_VIDEOS, Statement.RETURN_GENERATED_KEYS)){
            ps.setString(1, name);
            ps.setInt(2, (int) post.getId());
            ps.executeUpdate();
        }
    }



    public Post getPostById(long id) throws BadRequestException {
        Optional<Post> optionalPost = postRepository.findById(id);
        if (optionalPost.isPresent()){
            return optionalPost.get();
        }
        throw new BadRequestException("Sorry, that post doesn't exist");
    }

    public void save(Post post) {
        postRepository.save(post);
    }
    public ArrayList<ViewPostDTO> getPostsByUsername(String username) throws SQLException {
        Connection connection = jdbcTemplate.getDataSource().getConnection();
        try(PreparedStatement ps = connection.prepareStatement(GET_POSTS_BY_USERNAME, Statement.RETURN_GENERATED_KEYS)){
            ps.setString(1, username);
            ResultSet set = ps.executeQuery();
            ArrayList<ViewPostDTO> arr = new ArrayList<>();
            while(set.next()){
                ViewPostDTO postDTO = new ViewPostDTO();
                postDTO.setMapUrl(set.getString("map_url"));
                postDTO.setCoordinates(set.getString("coordinates"));
                postDTO.setLocationName(set.getString("location_name"));
                postDTO.setUserId(set.getInt("user_id"));
                postDTO.setCategoryId(set.getInt("category_id"));
                postDTO.setVideoUrl(set.getString("video_url"));
                postDTO.setOtherInfo(set.getString("other_info"));
                arr.add(postDTO);
            }
            return arr;

        }
    }
}
