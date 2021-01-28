package com.web.project.dao.meeting;

import com.web.project.model.meeting.Meeting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetingDao extends JpaRepository<Meeting, String>{
	
}
