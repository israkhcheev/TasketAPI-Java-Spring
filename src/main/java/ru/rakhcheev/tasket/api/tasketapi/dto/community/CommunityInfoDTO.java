package ru.rakhcheev.tasket.api.tasketapi.dto.community;

import lombok.Data;
import ru.rakhcheev.tasket.api.tasketapi.entity.CommunityEntity;

@Data
public class CommunityInfoDTO {

    private Long communityID;
    private String communityName;
    private String creatorLogin;
    private Boolean isPrivate;
    private int countOfUsers;

    public static CommunityInfoDTO toDTO(CommunityEntity entity) {
        CommunityInfoDTO communityDTO = new CommunityInfoDTO();
        communityDTO.setCommunityID(entity.getId());
        communityDTO.setCommunityName(entity.getCommunityName());
        communityDTO.setCreatorLogin(entity.getCreator().getLogin());
        communityDTO.setIsPrivate(entity.getIsPrivate());
        communityDTO.setCountOfUsers(entity.getUsersSet().size());
        return communityDTO;
    }

}
