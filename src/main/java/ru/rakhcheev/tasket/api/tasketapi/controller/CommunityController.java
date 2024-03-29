package ru.rakhcheev.tasket.api.tasketapi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.rakhcheev.tasket.api.tasketapi.dto.community.*;
import ru.rakhcheev.tasket.api.tasketapi.dto.invite.InviteUrlCreationDTO;
import ru.rakhcheev.tasket.api.tasketapi.dto.invite.InviteUrlDTO;
import ru.rakhcheev.tasket.api.tasketapi.exception.*;
import ru.rakhcheev.tasket.api.tasketapi.services.CommunityService;

import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping(value = "/community")
public class CommunityController {

    private final CommunityService communityService;

    @Autowired
    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @PostMapping
    public ResponseEntity<String> addCommunityRequest(@RequestBody CommunityCreationDTO community,
                                                      Authentication authentication) {
        try {
            communityService.addCommunity(community, authentication);
            return new ResponseEntity<>("Группа" + community.getCommunityName() + " добавлена.", HttpStatus.OK);
        } catch (UserHasNotPermission e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (AlreadyExistException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Произошла непредвиденная ошибка: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<?> getCommunities(@RequestParam(value = "type", defaultValue = "public") String type,
                                            Authentication authentication) {
        try {
            List<CommunityInfoDTO> communityList = communityService.getCommunities(type, authentication);
            return new ResponseEntity<>(communityList, HttpStatus.OK);
        } catch (CommunityEnumTypeIsNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Произошла непредвиденная ошибка: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<?> getCommunityById(@PathVariable(value = "id") Long id,
                                              Authentication authentication) {
        try {
            CommunityDTO communityList = communityService.getCommunityById(id, authentication);
            return new ResponseEntity<>(communityList, HttpStatus.OK);
        } catch (NotFoundException | UserHasNotPermission e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Произошла непредвиденная ошибка: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<String> deleteCommunityById(@PathVariable(value = "id") Long id,
                                                      Authentication authentication) {
        try {
            communityService.deleteCommunityById(id, authentication);
            return new ResponseEntity<>("Группа успешно удалена", HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (UserHasNotPermission e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>("Произошла непредвиденная ошибка: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(value = "/url")
    public ResponseEntity<?> addUrlForJoinCommunityById(@RequestBody InviteUrlCreationDTO communityCreateUrlDTO,
                                                        Authentication authentication) {
        try {
            InviteUrlDTO communityUrlDTO = communityService.addInviteUrl(
                    communityCreateUrlDTO,
                    authentication
            );
            return new ResponseEntity<>(communityUrlDTO, HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (UserHasNotPermission e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (CommunityHasTooManyUrlsException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (DateTimeParseException e) {
            return new ResponseEntity<>("Неверный формат времени (пример: 2018-05-05T11:50:55.1234)", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Произошла непредвиденная ошибка: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<String> updateCommunityDataByCommunityId(@PathVariable(value = "id") Long id,
                                                                   @RequestBody CommunityCreationDTO communityCreationDTO,
                                                                   Authentication authentication) {
        try {
            communityService.updateCommunity(id, communityCreationDTO, authentication);
            return new ResponseEntity<>("Данные группы изменены", HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (UserHasNotPermission e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>("Произошла непредвиденная ошибка: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(value = "/join/{id}")
    public ResponseEntity<String> joinPublicCommunityByAuthentication(@PathVariable(value = "id") Long id,
                                                                      Authentication authentication) {
        try {
            communityService.joinPublicCommunity(id, authentication);
            return new ResponseEntity<>("Пользователь добавлен в группу", HttpStatus.OK);
        } catch (AlreadyExistException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (UserHasNotPermission e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>("Произошла непредвиденная ошибка: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(value = "/join/{inviteKey}")
    public ResponseEntity<String> joinCommunityWithInviteKey(@PathVariable(value = "inviteKey") String inviteKey,
                                                             Authentication authentication) {
        try {
            communityService.joinWithInviteKey(inviteKey, authentication);
            return new ResponseEntity<>("Пользователь добавлен в группу", HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (AlreadyExistException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Произошла непредвиденная ошибка: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}