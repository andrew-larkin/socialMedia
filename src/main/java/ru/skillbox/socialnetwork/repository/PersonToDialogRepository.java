package ru.skillbox.socialnetwork.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.skillbox.socialnetwork.model.entity.Dialog;
import ru.skillbox.socialnetwork.model.entity.Person;
import ru.skillbox.socialnetwork.model.entity.PersonToDialog;

import java.util.List;
import java.util.Optional;

public interface PersonToDialogRepository extends JpaRepository<PersonToDialog, Long> {

    List<PersonToDialog> findByDialog(Dialog dialog);

    List<PersonToDialog> findByPerson(Person person);

    Optional<PersonToDialog> findByDialogAndPerson(Dialog dialog, Person person);
}