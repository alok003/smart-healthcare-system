package com.project.doctorService.Entity;

import com.project.doctorService.Model.Gender;
import com.project.doctorService.Model.Specialization;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.util.List;
@Data
@Document(collection = "Doctors")
public class Doctor {
    @Id
    private String id;
    private String email;
    @Field(targetType = FieldType.STRING)
    private Gender gender;
    private List<Specialization> specializations;
    private String licenseNumber;
    private String contactNumber;
    private String overview;
}
