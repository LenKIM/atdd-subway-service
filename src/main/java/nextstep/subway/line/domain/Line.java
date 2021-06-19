package nextstep.subway.line.domain;

import nextstep.subway.BaseEntity;
import nextstep.subway.station.domain.Station;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
public class Line extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String name;
    private String color;

    @Embedded
    private final Sections sections = new Sections(new ArrayList<>());

    public Line() {
    }

    public Line(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public Line(String name, String color, Station upStation, Station downStation, int distance) {
        this.name = name;
        this.color = color;
        sections.add(new Section(this, upStation, downStation, distance));
    }

    public void update(Line line) {
        this.name = line.getName();
        this.color = line.getColor();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public Sections getSections() {
        return sections;
    }

    public List<Station> getStations() {
        return this.getSections().getStations();
    }

    public void addSection(Section section) {
        Station downStation = section.getDownStation();
        Station upStation = section.getUpStation();
        boolean isUpStationExisted = getStations().stream().anyMatch(it -> it == upStation);
        boolean isDownStationExisted = getStations().stream().anyMatch(it -> it == downStation);

        validateBothStationsRegistered(isUpStationExisted, isDownStationExisted);
        validateBothStationsNotRegistered(isUpStationExisted, isDownStationExisted);

        if (getStations().isEmpty()) {
            getSections().add(section);
            return;
        }

        if (isUpStationExisted) {
            getSections().stream()
                    .filter(it -> it.getUpStation() == upStation)
                    .findFirst()
                    .ifPresent(it -> it.updateUpStation(downStation, section.getDistance()));

            getSections().add(section);
            return;
        }

        if (isDownStationExisted) {
            getSections().stream()
                    .filter(it -> it.getDownStation() == downStation)
                    .findFirst()
                    .ifPresent(it -> it.updateDownStation(upStation, section.getDistance()));
            getSections().add(section);
            return;
        }

        getSections().add(section);

    }

    private void validateBothStationsNotRegistered(boolean downStation, boolean upStation) {
        if ((getStations().isEmpty() == false)
                && (downStation == false)
                && upStation == false) {
            throw new RuntimeException("등록할 수 없는 구간 입니다.");
        }
    }

    private void validateBothStationsRegistered(boolean isUpStationExisted, boolean isDownStationExisted) {
        if (isUpStationExisted && isDownStationExisted) {
            throw new RuntimeException("이미 등록된 구간 입니다.");
        }
    }

    public void removeLineStation(Station station) {
        getSections().exist();

        Optional<Section> upStation = getSections().getStationInUpStations(station);
        Optional<Section> downStation = getSections().getStationInDownStations(station);
        if (upStation.isPresent() && downStation.isPresent()) {
            Station newUpStation = downStation.get().getUpStation();
            Station newDownStation = upStation.get().getDownStation();
            int newDistance = upStation.get().getDistance() + downStation.get().getDistance();
            Section section = new Section(this, newUpStation, newDownStation, newDistance);
            getSections().add(section);
        }

        upStation.ifPresent(it -> getSections().remove(it));
        downStation.ifPresent(it -> getSections().remove(it));
    }
}
