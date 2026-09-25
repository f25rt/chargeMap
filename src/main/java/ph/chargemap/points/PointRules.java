package ph.chargemap.points;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Singleton config for the points engine (design.md). Fixed id so there is exactly one.
 */
@Document(collection = "point_rules")
public class PointRules {

    public static final String SINGLETON_ID = "points-config";

    @Id
    private String id = SINGLETON_ID;

    private int pointsPerStationAdd = 50;
    private int pointsPerStationUpdate = 10;
    private int pointsPerReport = 5;

    private int dailyCapStationAdd = 200;
    private int dailyCapStationUpdate = 100;
    private int dailyCapReport = 50;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getPointsPerStationAdd() {
        return pointsPerStationAdd;
    }

    public void setPointsPerStationAdd(int v) {
        this.pointsPerStationAdd = v;
    }

    public int getPointsPerStationUpdate() {
        return pointsPerStationUpdate;
    }

    public void setPointsPerStationUpdate(int v) {
        this.pointsPerStationUpdate = v;
    }

    public int getPointsPerReport() {
        return pointsPerReport;
    }

    public void setPointsPerReport(int v) {
        this.pointsPerReport = v;
    }

    public int getDailyCapStationAdd() {
        return dailyCapStationAdd;
    }

    public void setDailyCapStationAdd(int v) {
        this.dailyCapStationAdd = v;
    }

    public int getDailyCapStationUpdate() {
        return dailyCapStationUpdate;
    }

    public void setDailyCapStationUpdate(int v) {
        this.dailyCapStationUpdate = v;
    }

    public int getDailyCapReport() {
        return dailyCapReport;
    }

    public void setDailyCapReport(int v) {
        this.dailyCapReport = v;
    }
}
