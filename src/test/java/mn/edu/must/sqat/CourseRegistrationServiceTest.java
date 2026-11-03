package mn.edu.must.sqat;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    private static final String COURSE = "F.CSM202";
    private static final String PREREQ = "S.CFM100";

    private static final Student ACTIVE = new Student("B232270901", "b232270901@must.edu.mn", true, List.of(PREREQ));
    private static final Student INACTIVE = new Student("B232270902", "b232270902@must.edu.mn", false, List.of(PREREQ));
    private static final Student NO_PREREQ = new Student("B232270903", "b232270903@must.edu.mn", true, List.of());

    @Mock
    StudentRepository repository;
    @Mock
    EmailSender emailSender;
    CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    // A1
    @Test
    void register_activeStudent_returnsOkSavesAndSendsEmail() {
        when(repository.findStudent("B232270901")).thenReturn(ACTIVE);
        when(repository.courseExists(COURSE)).thenReturn(true);
        when(repository.prerequisites(COURSE)).thenReturn(List.of(PREREQ));

        assertEquals("OK", service.register("B232270901", COURSE));

        verify(repository).saveRegistration("B232270901", COURSE);
        verify(emailSender).send(eq("b232270901@must.edu.mn"), anyString(), contains(COURSE));
    }

    // A2
    @Test
    void register_unknownStudent_returnsNoStudentAndDoesNotSaveOrEmail() {
        when(repository.findStudent("B232270999")).thenReturn(null);

        assertEquals("ERROR_NO_STUDENT", service.register("B232270999", COURSE));

        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // A3
    @Test
    void register_inactiveStudent_returnsInactiveAndNeverTouchesEmail() {
        when(repository.findStudent("B232270902")).thenReturn(INACTIVE);

        assertEquals("ERROR_INACTIVE_STUDENT", service.register("B232270902", COURSE));

        verifyNoInteractions(emailSender);
    }

    // A4
    @Test
    void register_missingPrerequisite_returnsPrerequisitesErrorAndDoesNotSave() {
        when(repository.findStudent("B232270903")).thenReturn(NO_PREREQ);
        when(repository.courseExists(COURSE)).thenReturn(true);
        when(repository.prerequisites(COURSE)).thenReturn(List.of(PREREQ));

        assertEquals("ERROR_PREREQUISITES", service.register("B232270903", COURSE));

        verify(repository, never()).saveRegistration(anyString(), anyString());
    }

    // A5
    @Test
    void register_emailSenderThrows_registrationStillOkAndSaved() {
        when(repository.findStudent("B232270901")).thenReturn(ACTIVE);
        when(repository.courseExists(COURSE)).thenReturn(true);
        when(repository.prerequisites(COURSE)).thenReturn(List.of(PREREQ));
        doThrow(new RuntimeException("SMTP down"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        assertEquals("OK", service.register("B232270901", COURSE));

        verify(repository).saveRegistration("B232270901", COURSE);
    }

    // A6
    @Test
    void register_nullArguments_throwsIllegalArgumentAndNeverTouchesDependencies() {
        assertThrows(IllegalArgumentException.class, () -> service.register(null, COURSE));
        assertThrows(IllegalArgumentException.class, () -> service.register("B232270901", null));
        verifyNoInteractions(repository, emailSender);
    }

    // A7
    @Test
    void register_success_emailBodyContainsCourseCode() {
        when(repository.findStudent("B232270901")).thenReturn(ACTIVE);
        when(repository.courseExists(COURSE)).thenReturn(true);
        when(repository.prerequisites(COURSE)).thenReturn(List.of(PREREQ));

        service.register("B232270901", COURSE);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq("b232270901@must.edu.mn"), anyString(), body.capture());
        assertTrue(body.getValue().contains(COURSE));
    }

    // A8
    @ParameterizedTest(name = "exists={0}, active={1}, prereqTaken={2} -> {3}")
    @CsvSource({
            "false, true,  true,  ERROR_NO_STUDENT",
            "true,  false, true,  ERROR_INACTIVE_STUDENT",
            "true,  true,  false, ERROR_PREREQUISITES"
    })
    void register_errorCases_returnExpectedCode(boolean exists, boolean active,
            boolean prereqTaken, String expected) {
        Student s = exists
                ? new Student("B232270901", "b232270901@must.edu.mn", active,
                        prereqTaken ? List.of(PREREQ) : List.of())
                : null;
        when(repository.findStudent("B232270901")).thenReturn(s);
        lenient().when(repository.courseExists(COURSE)).thenReturn(true);
        lenient().when(repository.prerequisites(COURSE)).thenReturn(List.of(PREREQ));

        assertEquals(expected, service.register("B232270901", COURSE));

        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }
}