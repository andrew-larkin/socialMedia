package ru.skillbox.socialnetwork.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import ru.skillbox.socialnetwork.api.requests.EmailPassPassFirstNameLastNameCodeRequest;
import ru.skillbox.socialnetwork.api.requests.EmailRequest;
import ru.skillbox.socialnetwork.api.requests.NotificationTypeEnableRequest;
import ru.skillbox.socialnetwork.api.requests.TokenPasswordRequest;
import ru.skillbox.socialnetwork.api.responses.*;
import ru.skillbox.socialnetwork.model.entity.NotificationSettings;
import ru.skillbox.socialnetwork.model.entity.NotificationType;
import ru.skillbox.socialnetwork.model.entity.Person;
import ru.skillbox.socialnetwork.repository.NotificationSettingsRepository;
import ru.skillbox.socialnetwork.repository.NotificationTypeRepository;
import ru.skillbox.socialnetwork.repository.PersonRepository;
import ru.skillbox.socialnetwork.security.JwtTokenProvider;
import ru.skillbox.socialnetwork.security.PersonDetailsService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private final PersonRepository personRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final NotificationTypeRepository notificationTypeRepository;
    private final EmailSenderService emailSenderService;
    private final BCryptPasswordEncoder encoder;
    private final PersonDetailsService personDetailsService;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public AccountService(PersonRepository personRepository,
                          NotificationSettingsRepository notificationSettingsRepository,
                          NotificationTypeRepository notificationTypeRepository,
                          EmailSenderService emailSenderService,
                          BCryptPasswordEncoder encoder,
                          PersonDetailsService personDetailsService,
                          JwtTokenProvider jwtTokenProvider) {
        this.personRepository = personRepository;
        this.notificationSettingsRepository = notificationSettingsRepository;
        this.notificationTypeRepository = notificationTypeRepository;
        this.emailSenderService = emailSenderService;
        this.encoder = encoder;
        this.personDetailsService = personDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
    }


    public Optional<Person> findPersonByEmail(String email) {
        return personRepository.findByEmail(email);
    }

    private boolean isEmailCorrect(String email) {
        if (email == null || email.length() < 6) return false;
        return email.toLowerCase()
                .replaceAll("(^([a-z0-9_\\.-]+)@([a-z0-9_\\.-]+)\\.([a-z\\.]{2,6})$)", "")
                .equals("");
    }

    private boolean isNameCorrect(String name) {
        if (name == null || name.length() < 3) return false;
        return name.toLowerCase()
                .replaceAll("(^[a-zа-яё0-9-]+$)", "").equals("");
    }

    public void setDefaultNotifySettings(Person per) {
        List<NotificationType> nt = notificationTypeRepository.findAll();
        try {
            for (NotificationType notificationType : nt) {
                notificationSettingsRepository
                        .save(NotificationSettings.builder()
                                .notificationType(notificationType)
                                .personNS(per)
                                .enable(1)
                                .build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean savePerson(EmailPassPassFirstNameLastNameCodeRequest requestBody) {
        personRepository.save(new Person(
                requestBody.getEmail(),
                encoder.encode(requestBody.getPasswd1()),
                requestBody.getFirstName(),
                requestBody.getLastName(),
                LocalDateTime.now()));
        Optional<Person> personOptional = personRepository.findByEmail(requestBody.getEmail());
        if (personOptional.isPresent()) {
            setDefaultNotifySettings(personOptional.get());
            return true;
        }
        return false;
    }

    private void changePassword(Person person, String password) {
        person.setPassword(encoder.encode(password));
        person.setConfirmationCode(null);
        personRepository.save(person);
    }

    private void changeEmail(Person person, String email) {
        person.setEmail(email);
        person.setConfirmationCode("");
        personRepository.save(person);
    }

    public ResponseEntity<?> postApiAccountRegister(EmailPassPassFirstNameLastNameCodeRequest requestBody) {
        StringBuilder errors = new StringBuilder();

        String email = requestBody.getEmail();
        if (!isEmailCorrect(email)) {
            errors.append(" Wrong email! ");
        }
        if (findPersonByEmail(email).isPresent()) {
            errors.append(" This email is already registered! ");
        }
        if (!requestBody.getPasswd1().equals(requestBody.getPasswd2())) {
            errors.append(" Passwords not equals! ");
        }

        if (!isNameCorrect(requestBody.getFirstName()) ||
                !isNameCorrect(requestBody.getLastName())) {
            errors.append(" Firstname or last name is incorrect! ");
        }

        if (!errors.toString().equals("")) {
            return ResponseEntity.status(400).body(new ErrorErrorDescriptionResponse(errors.toString()));
        }

        if (savePerson(requestBody)) {
            return ResponseEntity.status(200)
                    .body(new ErrorTimeDataResponse("", new MessageResponse()));
        }

        return ResponseEntity.status(400).body(new ErrorErrorDescriptionResponse("Can't save user!"));
    }

    public ResponseEntity<?> putApiAccountPasswordRecovery(EmailRequest requestBody) {
        Optional<Person> optionalPerson = findPersonByEmail(requestBody.getEmail());

        if (optionalPerson.isEmpty()) {
            return ResponseEntity.status(400)
                    .body(new ErrorErrorDescriptionResponse("This email is not registered!"));
        }

        Person person = optionalPerson.get();
        String pass = encoder.encode(Long.toString(System.currentTimeMillis()))
                .substring(10).replaceAll("\\W", "");

        if (emailSenderService.sendEmailChangePassword(person, pass)) {
            /*
            Было принято решение вместо ссылки на страницу восстановления пароля отправлять в письме
            уже сгенерированный пароль.
            Поэтому это выражение пока не действительно:
            setConfirmationCode(person, confirmationCode);
             */
            changePassword(person, pass);
            return ResponseEntity.status(200)
                    .body(new ErrorTimeDataResponse("", new MessageResponse()));
        }

        return ResponseEntity.status(400)
                .body(new ErrorErrorDescriptionResponse("Error sending email"));
    }

    public ResponseEntity<?> putApiAccountPasswordSet(TokenPasswordRequest requestBody) {
        String token = requestBody.getToken();
        if (token == null || token.length() == 0) {
            return ResponseEntity.status(400).body(new ErrorErrorDescriptionResponse("Bad token!"));
        }

        String password = requestBody.getPassword().getPassword();
        if (password == null || password.length() < 8) {
            return ResponseEntity.status(400).body(new ErrorErrorDescriptionResponse("Invalid password!"));
        }

        Person person = personDetailsService.getCurrentUser();

        if (!person.getEmail().equals(jwtTokenProvider.getLoginFromToken(token.substring(6).trim()))) {
            return ResponseEntity.status(400).body(new ErrorErrorDescriptionResponse("Token is expired!"));
        }

        changePassword(person, password);

        return ResponseEntity.status(200)
                .body(new ErrorTimeDataResponse("", new MessageResponse()));
    }

    public ResponseEntity<?> putApiAccountEmail(EmailRequest requestBody) {
        Person currentUser = personDetailsService.getCurrentUser();
        String newEmail = requestBody.getEmail();

        if (!isEmailCorrect(newEmail) || currentUser.getEmail().equals(newEmail)) {
            return ResponseEntity.status(400).body(new ErrorErrorDescriptionResponse("Email is not valid!"));
        }

        changeEmail(currentUser, newEmail);

        return ResponseEntity.status(200)
                .body(new ErrorTimeDataResponse("", new MessageResponse()));
    }

    public ResponseEntity<?> putApiAccountNotifications(NotificationTypeEnableRequest requestBody) {

        Optional<NotificationType> notificationType =
                notificationTypeRepository.findByName(requestBody.getNotificationType());
        if (notificationType.isEmpty()) {
            return ResponseEntity.status(400).body(new ErrorErrorDescriptionResponse("Wrong notification type"));
        }

        Person person = personDetailsService.getCurrentUser();
        boolean isEnable = requestBody.isEnable();

        Optional<NotificationSettings> notificationSettingsOptional = notificationSettingsRepository
                .findByPersonNSAndNotificationTypeId(person, notificationType.get().getId());

        if (notificationSettingsOptional.isPresent()) {
            NotificationSettings notificationSetting = notificationSettingsOptional.get();
            notificationSetting.setEnable(isEnable);
            notificationSettingsRepository.save(notificationSetting);
            return ResponseEntity.status(200)
                    .body(new ErrorTimeDataResponse("", new MessageResponse()));
        }

        return ResponseEntity.status(400).body(new ErrorErrorDescriptionResponse("Setting not found!"));
    }

    public ResponseEntity<?> getApiAccountNotifications() {
        List<EnableTypeResponse> result = new ArrayList<>();

        for (NotificationSettings setting :
                notificationSettingsRepository.findByPersonNS(personDetailsService.getCurrentUser())) {

            result.add(new EnableTypeResponse(setting.getIsEnable(), setting.getNotificationType().getName()));
        }

        return ResponseEntity.status(200).body(new ErrorTimeListDataResponse(result));
    }
}