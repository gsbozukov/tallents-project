package ittalentss11.traveller_online.model.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class User {
    private long id;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String password;
}