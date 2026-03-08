package mg.miniframework.persistence.utils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ExportFilters {
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Integer depotId;
    private Integer statutId;
    private String search;

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(LocalDate dateFrom) {
        this.dateFrom = dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public void setDateTo(LocalDate dateTo) {
        this.dateTo = dateTo;
    }

    public Integer getDepotId() {
        return depotId;
    }

    public void setDepotId(Integer depotId) {
        this.depotId = depotId;
    }

    public Integer getStatutId() {
        return statutId;
    }

    public void setStatutId(Integer statutId) {
        this.statutId = statutId;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("dateFrom", dateFrom);
        map.put("dateTo", dateTo);
        map.put("depotId", depotId);
        map.put("statutId", statutId);
        map.put("search", search);
        return map;
    }

    public static ExportFilters fromParams(String dateFrom, String dateTo, Integer depotId, Integer statutId,
            String search) {
        ExportFilters filters = new ExportFilters();
        filters.setDateFrom(parseDate(dateFrom));
        filters.setDateTo(parseDate(dateTo));
        filters.setDepotId(depotId);
        filters.setStatutId(statutId);
        filters.setSearch(search);
        return filters;
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
