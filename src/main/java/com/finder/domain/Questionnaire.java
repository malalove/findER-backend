package com.finder.domain;

import com.finder.dto.QuestionnaireDto;
import lombok.*;
import javax.persistence.*;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Questionnaire extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "QUESTIONNAIRE_SEQUENCE_GENERATOR")
    @SequenceGenerator(name = "QUESTIONNAIRE_SEQUENCE_GENERATOR", sequenceName = "QUESTIONNAIRE_SQ", initialValue = 1, allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "USER_ID")
    private Users user;

    private String name;

    private String birthday;

    private String familyRelations;

    private String phoneNum;

    private String address;

    private String gender;

    private String bloodType;

    private String allergy;

    private String medicine;

    private String smokingCycle;

    private String drinkingCycle;

    private String etc;

    public static Questionnaire convertToQuestionnaire(QuestionnaireDto questionnaireDto, Users user) {
        return Questionnaire.builder()
                .user(user)
                .name(questionnaireDto.getName())
                .birthday(questionnaireDto.getBirthday())
                .familyRelations(questionnaireDto.getFamilyRelations())
                .phoneNum(questionnaireDto.getPhoneNum())
                .address(questionnaireDto.getAddress())
                .gender(questionnaireDto.getGender())
                .bloodType(questionnaireDto.getBloodType())
                .allergy(questionnaireDto.getAllergy())
                .medicine(questionnaireDto.getMedicine())
                .smokingCycle(questionnaireDto.getSmokingCycle())
                .drinkingCycle(questionnaireDto.getDrinkingCycle())
                .etc(questionnaireDto.getEtc())
                .build();
    }
}