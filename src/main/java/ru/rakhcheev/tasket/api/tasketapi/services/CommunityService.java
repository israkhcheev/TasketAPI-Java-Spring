package ru.rakhcheev.tasket.api.tasketapi.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import ru.rakhcheev.tasket.api.tasketapi.dto.community.*;
import ru.rakhcheev.tasket.api.tasketapi.dto.invite.InviteUrlCreationDTO;
import ru.rakhcheev.tasket.api.tasketapi.dto.invite.InviteUrlDTO;
import ru.rakhcheev.tasket.api.tasketapi.entity.*;
import ru.rakhcheev.tasket.api.tasketapi.entity.enums.CommunitySearchTypeEnum;
import ru.rakhcheev.tasket.api.tasketapi.entity.enums.EntityStatusEnum;
import ru.rakhcheev.tasket.api.tasketapi.exception.*;
import ru.rakhcheev.tasket.api.tasketapi.repository.CommunityRepo;
import ru.rakhcheev.tasket.api.tasketapi.repository.InviteUrlRepo;
import ru.rakhcheev.tasket.api.tasketapi.repository.UserRepo;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CommunityService {

    private final UserRepo userRepo;
    private final CommunityRepo communityRepo;
    private final InviteUrlRepo inviteUrlRepo;

    @Value("${community.count.max}")
    private int COUNT_OF_MAX_COMMUNITIES;

    @Autowired
    public CommunityService(UserRepo userRepo, CommunityRepo communityRepo, InviteUrlRepo communityUrlRepo) {
        this.userRepo = userRepo;
        this.communityRepo = communityRepo;
        this.inviteUrlRepo = communityUrlRepo;
    }

    public void addCommunity(CommunityCreationDTO community, Authentication authentication)
            throws UserHasNotPermission, AlreadyExistException {

        UserEntity user = userRepo.findByLogin(authentication.getName());

        if (!canCreateCommunity(user)) throw new UserHasNotPermission("Исчерпан лимит созданных груп");
        if (communityRepo.findByCommunityName(community.getCommunityName()) != null)
            throw new AlreadyExistException("Название \"" + community.getCommunityName() + "\" занято.");

        CommunityEntity communityEntity = CommunityCreationDTO.toEntity(community);
        communityEntity.setCreator(user);

        Set<UserEntity> userEntitySet = new HashSet<>();
        userEntitySet.add(user);
        communityEntity.setUsersSet(userEntitySet);

        communityRepo.save(communityEntity);
    }


    public List<CommunityInfoDTO> getCommunities(String type, Authentication authentication)
            throws CommunityEnumTypeIsNotFoundException {
        CommunitySearchTypeEnum communityTypeEnum;
        Iterable<CommunityEntity> communitiesIterableList;
        List<CommunityInfoDTO> communitiesList;
        UserEntity user;

        try {
            communityTypeEnum = Enum.valueOf(CommunitySearchTypeEnum.class, type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CommunityEnumTypeIsNotFoundException("Аргумент \"type\": " + type + " не поддерживается.");
        }

        communitiesList = new ArrayList<>();
        communitiesIterableList = communityRepo.findAll();

        switch (communityTypeEnum) {
            case JOINED:
                user = userRepo.findByLogin(authentication.getName());

                for (CommunityEntity community : user.getGroupSet())
                    if (community.getStatusActivity().ordinal() != 3)
                        communitiesList.add(CommunityInfoDTO.toDTO(community));

                return communitiesList;
            case PUBLIC:
                for (CommunityEntity community : communitiesIterableList)
                    if (!community.getIsPrivate() && (community.getStatusActivity().ordinal() != 3))
                        communitiesList.add(CommunityInfoDTO.toDTO(community));

                return communitiesList;
            case CREATED:
                user = userRepo.findByLogin(authentication.getName());
                for (CommunityEntity community : user.getSetOfCreatedGroups())
                    if (community.getStatusActivity().ordinal() != 3)
                        communitiesList.add(CommunityInfoDTO.toDTO(community));

                return communitiesList;
            default:
                for (CommunityEntity community : communitiesIterableList)
                    if (community.getStatusActivity().ordinal() != 3)
                        communitiesList.add(CommunityInfoDTO.toDTO(community));

                return communitiesList;
        }
    }

    public CommunityDTO getCommunityById(Long id, Authentication authentication)
            throws NotFoundException, UserHasNotPermission {
        CommunityEntity community = getCommunityEntity(id, authentication.getName());
        return CommunityDTO.toDTO(community);
    }

    public void deleteCommunityById(Long id, Authentication authentication)
            throws NotFoundException, UserHasNotPermission {
        UserEntity user = userRepo.findByLogin(authentication.getName());
        CommunityEntity community = getCommunityEntity(id, authentication.getName());

        if(user.getAuthority().ordinal() == 3) {
            communityRepo.delete(community);
            return;
        }
        if (!community.getCreator().equals(user))
            throw new UserHasNotPermission("Только создатель может удалить группу");
        community.setStatusActivity(EntityStatusEnum.DELETED);
        communityRepo.save(community);
    }

    public InviteUrlDTO addInviteUrl(InviteUrlCreationDTO communityCreateUrlDTO, Authentication authentication)
            throws CommunityHasTooManyUrlsException, NotFoundException, UserHasNotPermission {

        InviteUrlEntity url;
        UserEntity user = userRepo.findByLogin(authentication.getName());
        CommunityEntity community = getCommunityEntity(communityCreateUrlDTO.getCommunityId(), user.getLogin());
        String dateTimeString;

        if(!community.getCreator().equals(user))
            throw new UserHasNotPermission("Только создатель группы может создавать пригласительные ссылки");

        if (inviteUrlRepo.findAllByCommunity(community).size() >= 5) throw new CommunityHasTooManyUrlsException();

        url = new InviteUrlEntity(community);
        while (inviteUrlRepo.findByUrlParam(url.getUrlParam()) != null) url.regenerateUrlParam();

        dateTimeString = communityCreateUrlDTO.getDestroyDate();
        url.setDestroyDate(
                dateTimeString == null || dateTimeString.isBlank()
                        ? null
                        : LocalDateTime.parse(dateTimeString)
        );
        if (communityCreateUrlDTO.getOnceUsed() != null) url.setOnceUsed(communityCreateUrlDTO.getOnceUsed());
        inviteUrlRepo.save(url);
        return InviteUrlDTO.toDTO(url);
    }

    public void joinWithInviteKey(String inviteKey, Authentication authentication)
            throws NotFoundException, AlreadyExistException {
        UserEntity user;
        InviteUrlEntity urlEntity = inviteUrlRepo.findByUrlParam(inviteKey);
        CommunityEntity community;

        if (urlEntity.getStatus().equals(EntityStatusEnum.DELETED))
            throw new NotFoundException("Данного ключа не существует, либо он был использован");
        if (urlEntity.getDestroyDate() != null && urlEntity.getDestroyDate().isBefore(LocalDateTime.now())) {
            urlEntity.setStatus(EntityStatusEnum.DELETED);
            throw new NotFoundException("Данного ключа не существует, либо он был использован");
        }

        user = userRepo.findByLogin(authentication.getName());
        community = urlEntity.getCommunity();

        if (community.getStatusActivity().ordinal() == 3)
            throw new NotFoundException("Группа с id: " + community.getId() + " не найдена.");

        if (community.getUsersSet().contains(user))
            throw new AlreadyExistException("Данный пользователь уже состоит в группе");

        community.getUsersSet().add(user);
        communityRepo.save(community);
        if (urlEntity.getOnceUsed()) {
            urlEntity.setStatus(EntityStatusEnum.DELETED);
            inviteUrlRepo.save(urlEntity);
        }
    }

    public void joinPublicCommunity(Long id, Authentication authentication)
            throws NotFoundException, UserHasNotPermission, AlreadyExistException {
        UserEntity user;
        CommunityEntity community;

        community = getCommunityEntity(id, authentication.getName());
        user = userRepo.findByLogin(authentication.getName());
        if (community.getUsersSet().contains(user))
            throw new AlreadyExistException("Данный пользователь уже состоит в группе");
        if (community.getIsPrivate())
            throw new UserHasNotPermission("Нет прав для доступа, так как данная группа приватная");

        community.getUsersSet().add(user);
        communityRepo.save(community);
    }

    public void updateCommunity(Long id, CommunityCreationDTO communityNewData, Authentication authentication)
            throws NotFoundException, UserHasNotPermission {

        CommunityEntity community = getCommunityEntity(id, authentication.getName());
        UserEntity user = userRepo.findByLogin(authentication.getName());

        if (communityNewData.getCommunityName() != null) community.setCommunityName(communityNewData.getCommunityName());
        if (communityNewData.getIsPrivate() != null) community.setIsPrivate(communityNewData.getIsPrivate());

        if (user.getAuthority().ordinal() == 3) {
            communityRepo.save(community);
            return;
        }

        if(!community.getCreator().equals(user))
            throw new UserHasNotPermission("Только создатель группы может обновлять данные группы");

        communityRepo.save(community);
    }

    private CommunityEntity getCommunityEntity(Long id, String userLogin)
            throws NotFoundException, UserHasNotPermission {
        UserEntity user;
        CommunityEntity community;
        Optional<CommunityEntity> communityEntityOptional = communityRepo.findById(id);
        if (communityEntityOptional.isEmpty())
            throw new NotFoundException("Группа с id: " + id + " не найдена или удалена.");

        community = communityEntityOptional.get();
        if (community.getStatusActivity().ordinal() == 3)
            throw new NotFoundException("Группы с id: " + id + " не найдена или удалена.");
        user = userRepo.findByLogin(userLogin);

        // отрицание прямой импликации
        if (community.getIsPrivate() && !community.getUsersSet().contains(user))
            throw new UserHasNotPermission("Нет прав для доступа к группе");

        return community;
    }

    private boolean canCreateCommunity(UserEntity user) {

        int countOfCreatedGroups = user.getSetOfCreatedGroups().size();

        if (countOfCreatedGroups == 0) return true;
        if (user.getAuthority().ordinal() < 2) return false;

        return countOfCreatedGroups < COUNT_OF_MAX_COMMUNITIES;
    }
}
