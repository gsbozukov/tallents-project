package ittalentss11.traveller_online.model.pojo;

import lombok.*;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Component
@Entity
@Table(name = "posts")

public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn (name = "user_id")
    private User user;
    @ManyToOne
    @JoinColumn (name = "category_id")
    private Category category;
    @Column
    private String videoUrl;
    @Column
    private String description;
    @Column
    private String otherInfo;
    @Column
    private String coordinates;
    @Column
    private String mapUrl;
    @Column
    private String locationName;
    @Column
    private LocalDateTime dateTime;

    //for likes
    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE
    })
    @JoinTable(name = "likes",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> usersThatLiked = new HashSet<>();

    //for dislikes
    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE
    })
    @JoinTable(name = "dislikes",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> usersThatDisliked = new HashSet<>();
    //for tags
    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE
    })
    @JoinTable(name = "tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> usersTagged = new HashSet<>();

    public void addLikeByUser(User user) {
        usersThatLiked.add(user);
        user.getLikedPosts().add(this);
    }
    public void removeLikeByUser(User user) {
        usersThatLiked.remove(user);
        user.getLikedPosts().remove(this);
    }
    public boolean isLikedByUser (User user){
        return this.usersThatLiked.contains(user);
    }
    public void addDislikeByUser(User user) {
        usersThatDisliked.add(user);
        user.getDislikedPosts().add(this);
    }
    public void removeDislikeByUser(User user) {
        usersThatDisliked.remove(user);
        user.getDislikedPosts().remove(this);
    }
    public boolean isDislikedByUser (User user){
        return this.usersThatDisliked.contains(user);
    }
    public void addTaggedUser(User user) {
        usersTagged.add(user);
        user.getTaggedInPosts().add(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return id == post.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
