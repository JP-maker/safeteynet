package com.openclassroom.safetynet.service;

import com.openclassroom.safetynet.dto.*;
import com.openclassroom.safetynet.model.FireStation;
import com.openclassroom.safetynet.model.MedicalRecord;
import com.openclassroom.safetynet.model.Person;
import com.openclassroom.safetynet.repository.FireStationRepository;
import com.openclassroom.safetynet.repository.MedicalRecordRepository;
import com.openclassroom.safetynet.repository.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class FireStationServiceTest {

    @Mock
    private FireStationRepository fireStationRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    @Mock
    private PersonService personService;

    @InjectMocks
    private FireStationService fireStationService;

    @Test
    void getPeopleCoveredByStation_shouldReturnCoverage() {
        int stationNumber = 1;
        List<String> addresses = List.of("123 Main St");
        Person person = new Person();
        person.setFirstName("John");
        person.setLastName("Doe");
        person.setAddress("123 Main St");
        person.setCity("City");
        person.setZip("12345");
        person.setPhone("111-222-3333");
        person.setEmail("email@example.com");

        MedicalRecord record = new MedicalRecord();
        record.setFirstName("John");
        record.setLastName("Doe");
        record.setBirthdate("01/01/1990");
        record.setMedications(List.of());
        record.setAllergies(List.of());

        when(fireStationRepository.findAddressesByStationNumber(stationNumber)).thenReturn(addresses);
        when(personRepository.findByAddressIn(addresses)).thenReturn(List.of(person));
        when(medicalRecordRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(record));

        Optional<FireStationCoverageDTO> result = fireStationService.getPeopleCoveredByStation(stationNumber);

        assertThat(result).isPresent();
        assertThat(result.get().getPeople()).hasSize(1);
        assertThat(result.get().getAdultCount()).isEqualTo(1);
        assertThat(result.get().getChildCount()).isEqualTo(0);
    }

    @Test
    void getPeopleCoveredByStation_shouldReturnEmptyIfNoAddress() {
        int stationNumber = 99;

        when(fireStationRepository.findAddressesByStationNumber(stationNumber)).thenReturn(Collections.emptyList());

        Optional<FireStationCoverageDTO> result = fireStationService.getPeopleCoveredByStation(stationNumber);

        assertThat(result).isEmpty();
    }

    @Test
    void getPhoneNumberByStation_shouldReturnPhoneNumbers() {
        int stationNumber = 1;
        List<String> addresses = List.of("123 Main St");
        Person person = new Person();
        person.setFirstName("Jane");
        person.setLastName("Doe");
        person.setAddress("123 Main St");
        person.setCity("City");
        person.setZip("12345");
        person.setPhone("999-888-7777");
        person.setEmail("email@example.com");

        when(fireStationRepository.findAddressesByStationNumber(stationNumber)).thenReturn(addresses);
        when(personRepository.findByAddressIn(addresses)).thenReturn(List.of(person));

        Optional<PhoneAlertDTO> result = fireStationService.getPhoneNumberByStation(stationNumber);

        assertThat(result).isPresent();
        assertThat(result.get().getPhones()).containsExactly("999-888-7777");
    }

    @Test
    void getPhoneNumberByStation_shouldReturnEmptyIfNoAddresses() {
        when(fireStationRepository.findAddressesByStationNumber(anyInt())).thenReturn(Collections.emptyList());

        Optional<PhoneAlertDTO> result = fireStationService.getPhoneNumberByStation(1);

        assertThat(result).isEmpty();
    }

    @Test
    void getListOfPersonsWithMedicalRecordsByListOfFireStation_shouldReturnData() {
        List<String> stationNumbers = List.of("1");
        List<String> addresses = List.of("123 Main St");
        FirePersonDTO firePersonDTO = new FirePersonDTO();
        firePersonDTO.setPersons(List.of(new PersonWithMedicalRecordDTO()));

        when(fireStationRepository.findAddressesByStationNumber(1)).thenReturn(addresses);
        when(personService.getPersonFireStationAndMedicalReportByAddress("123 Main St")).thenReturn(Optional.of(firePersonDTO));

        Optional<ListOfAddressWithListOfPersonWithMedicalRecordDTO> result =
                fireStationService.getListOfPersonsWithMedicalRecordsByListOfFireStation(stationNumbers);

        assertThat(result).isPresent();
        assertThat(result.get().getFireStationAddressPersonMedicalRecords()).hasSize(1);
    }

    @Test
    void addFireStation_shouldAddMapping() {
        FireStation fireStation = new FireStation();
        fireStation.setAddress("123 Main St");
        fireStation.setStation("1");

        when(fireStationRepository.existsByAddress("123 Main St")).thenReturn(false);
        when(fireStationRepository.save(fireStation)).thenReturn(fireStation);

        FireStation result = fireStationService.addFireStation(fireStation);

        assertThat(result).isEqualTo(fireStation);
    }

    @Test
    void addFireStation_shouldThrowExceptionIfAddressExists() {
        FireStation fireStation = new FireStation();
        fireStation.setAddress("123 Main St");
        fireStation.setStation("1");

        when(fireStationRepository.existsByAddress("123 Main St")).thenReturn(true);

        assertThatThrownBy(() -> fireStationService.addFireStation(fireStation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mapping existant pour l'adresse: 123 Main St");
    }

    @Test
    void updateFireStation_shouldUpdateMapping() {
        FireStation fireStation = new FireStation();
        fireStation.setStation("2");
        fireStation.setAddress("123 Main St");

        when(fireStationRepository.existsByAddress("123 Main St")).thenReturn(true);
        when(fireStationRepository.save(fireStation)).thenReturn(fireStation);

        Optional<FireStation> result = fireStationService.updateFireStation(fireStation);

        assertThat(result).isPresent();
        assertThat(result.get().getStation()).isEqualTo("2");
    }

    @Test
    void updateFireStation_shouldReturnEmptyIfAddressNotFound() {
        FireStation fireStation = new FireStation();
        fireStation.setAddress("Unknown St");
        fireStation.setStation("2");

        when(fireStationRepository.existsByAddress("Unknown St")).thenReturn(false);

        Optional<FireStation> result = fireStationService.updateFireStation(fireStation);

        assertThat(result).isEmpty();
    }

    @Test
    void deleteFireStationMapping_shouldDeleteSuccessfully() {
        when(fireStationRepository.deleteByAddress("123 Main St")).thenReturn(true);

        boolean result = fireStationService.deleteFireStationMapping("123 Main St");

        assertThat(result).isTrue();
    }

    @Test
    void deleteFireStationMapping_shouldReturnFalseForInvalidAddress() {
        boolean result = fireStationService.deleteFireStationMapping("");

        assertThat(result).isFalse();
    }

    @Test
    void getAllFireStations_shouldReturnList() {
        FireStation fireStation = new FireStation();
        fireStation.setStation("1");
        fireStation.setAddress("123 Main St");
        List<FireStation> fireStations = List.of(fireStation);
        when(fireStationRepository.findAll()).thenReturn(fireStations);

        List<FireStation> result = fireStationService.getAllFireStations();

        assertThat(result).hasSize(1);
    }
}