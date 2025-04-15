package com.openclassroom.safetynet.repository;

import com.openclassroom.safetynet.model.Person;
import com.openclassroom.safetynet.utils.ConvertToUpper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class PersonRepository {

    private static final Logger logger = LoggerFactory.getLogger(PersonRepository.class);
    private List<Person> persons = new ArrayList<>();
    private List<String> addressesUpper = new ArrayList<>();

    /**
     * Initialise ou met à jour les données des personnes.
     * @param personList La liste complète des personnes chargées.
     */
    public void setData(List<Person> personList) {
        if (personList != null) {
            this.persons = new ArrayList<>(personList);
            logger.info("{} enregistrements de personnes chargés.", this.persons.size());
        } else {
            this.persons = new ArrayList<>();
            logger.warn("La liste des personnes fournie pour l'initialisation est nulle.");
        }
    }

    /**
     * Trouve toutes les personnes résidant à une liste d'adresses donnée.
     * @param addresses La liste des adresses à rechercher.
     * @return Une liste des personnes trouvées, ou une liste vide.
     */
    public List<Person> findByAddressIn(List<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return Collections.emptyList(); // Retourne une liste vide immuable
        }
        addressesUpper = ConvertToUpper.convertList(addresses);
        return persons.stream()
                .filter(p -> addressesUpper.contains(p.getAddress().toUpperCase()))
                .collect(Collectors.toList());
    }

    /**
     * Trouve une personne par son prénom et nom.
     * @param firstName Prénom.
     * @param lastName Nom.
     * @return Un Optional contenant la personne si trouvée, sinon Optional vide.
     */
    public Optional<Person> findByFirstNameAndLastName(String firstName, String lastName) {
        return persons.stream()
                .filter(p -> Objects.equals(p.getFirstName(), firstName) && Objects.equals(p.getLastName(), lastName))
                .findFirst(); // Suppose qu'il n'y a pas de doublons nom/prénom exacts
    }

    /**
     * Trouve toutes les personnes résidant à une adresse spécifique.
     * @param address L'adresse à rechercher.
     * @return Une liste des personnes trouvées à cette adresse.
     */
    public List<Person> findByAddress(String address) {
        return persons.stream()
                .filter(p -> Objects.equals(p.getAddress(), address))
                .collect(Collectors.toList());
    }

    /**
     * Récupère toutes les personnes.
     * @return Une copie de la liste des personnes.
     */
    public List<Person> findAll() {
        return new ArrayList<>(persons);
    }

    /**
     * Trouve des personnes par nom.
     * @param lastName Nom.
     * @return Un Optional contenant la personne si trouvée, sinon Optional vide.
     */
    public List<Person> findByLastName(String lastName) {
        return persons.stream()
                .filter(p -> Objects.equals(p.getLastName().toUpperCase(), lastName))
                .collect(Collectors.toList());
    }

    /**
     * Trouve des personnes par ville.
     * @param city nom de la ville.
     * @return Un Optional contenant les personnes si trouvées, sinon Optional vide.
     */
    public List<Person> findByCity(String city) {
        return persons.stream()
                .filter(p -> Objects.equals(p.getCity().toUpperCase(), city))
                .collect(Collectors.toList());
    }

    /**
     * Sauver une nouvelle personne.
     * @param person class.
     * @return la personne créée
     */
    public Person save(Person person) {
        persons.removeIf(p -> p.getFirstName().equalsIgnoreCase(person.getFirstName())
                && p.getLastName().equalsIgnoreCase(person.getLastName()));
        persons.add(person);
        return person;
    }

    /**
     * Supprimer une personne en la trouvant par son nom et son prénom.
     * @param firstName prénom.
     * @param lastName nom.
     * @return un booléan indiquant si la personne a été supprimée.
     */
    public boolean deleteByFirstNameAndLastName(String firstName, String lastName) {
        return persons.removeIf(p -> p.getFirstName().equalsIgnoreCase(firstName)
                && p.getLastName().equalsIgnoreCase(lastName));
    }

    /**
     * Vérifier si une personne existe en la trouvant par son nom et son prénom.
     * @param firstName prénom.
     * @param lastName nom.
     * @return un booléan indiquant si la personne existe.
     */
    public boolean existsById(String firstName, String lastName) {
        return persons.stream()
                .anyMatch(p -> p.getFirstName().equalsIgnoreCase(firstName)
                        && p.getLastName().equalsIgnoreCase(lastName));
    }
}