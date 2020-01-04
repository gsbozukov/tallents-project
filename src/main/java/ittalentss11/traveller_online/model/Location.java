package ittalentss11.traveller_online.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class Location {
    private int id;
    private String name;
    private String coordinates;
    private String mapUrl;
}
