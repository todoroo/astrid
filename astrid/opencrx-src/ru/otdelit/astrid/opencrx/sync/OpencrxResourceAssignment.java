package ru.otdelit.astrid.opencrx.sync;

import java.util.Date;

public class OpencrxResourceAssignment {

    private String resourceId;
    private Date assignmentDate;
    private String assignmentId;

    public String getResourceId() {
        return resourceId;
    }
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
    public Date getAssignmentDate() {
        return assignmentDate;
    }
    public void setAssignmentDate(Date assignmentDate) {
        this.assignmentDate = assignmentDate;
    }
    public String getAssignmentId() {
        return assignmentId;
    }
    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }


}
