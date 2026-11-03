package mn.edu.must.sqat;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
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
}