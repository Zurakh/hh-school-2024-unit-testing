package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {

  @InjectMocks
  private LibraryManager libraryManager;

  @Mock
  private NotificationService notificationService;

  @Mock
  private UserService userService;


  @ParameterizedTest
  @CsvSource({
          "1, 1, 5",
          "2, 10, 7",
          "3, 40, 10",
          "4, -4, 4",
          "5, 0, 0",
          "6, 7, 0",
          "7, 7, -1"
  })
  void testLibraryManagerWithAddingSuccess(String bookId, int quantity1, int quantity2) {
    // Добавление новой книги
    libraryManager.addBook(bookId, quantity1);
    assertEquals(libraryManager.getAvailableCopies(bookId), quantity1);

    // Изменение количества книги
    libraryManager.addBook(bookId, quantity2);
    assertEquals(libraryManager.getAvailableCopies(bookId), quantity1 + quantity2);
  }


  @Test
  void libraryManagerShouldReturnFalseIfAccountInactive() {
    when(userService.isUserActive("0")).thenReturn(false);
    assertFalse(libraryManager.borrowBook("0", "0"));
    Mockito.verify(notificationService, only()).notifyUser("0", "Your account is not active.");
  }

  @Test
  void libraryManagerShouldReturnFalseIfBookNotPresentInLibrary() {
    when(userService.isUserActive("1")).thenReturn(true);
    assertFalse(libraryManager.borrowBook("0", "1"));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, -10})
  void libraryManagerShouldReturnFalseIfBurrowingBookQuantityIsLessOrEqualToZero(int quantity) {
    libraryManager.addBook("1", quantity);
    when(userService.isUserActive("1")).thenReturn(true);
    assertFalse(libraryManager.borrowBook("1", "1"));
  }


  @Test
  void testBurrowingSuccess() {
    String userId = "1";
    String bookId = "1";
    when(userService.isUserActive(userId)).thenReturn(true);
    libraryManager.addBook(bookId, 1);
    boolean burrowingSuccess = libraryManager.borrowBook(bookId, userId);

    assertTrue(burrowingSuccess);
    assertEquals(libraryManager.getAvailableCopies(bookId), 0);
    Mockito.verify(notificationService).notifyUser(userId, "You have borrowed the book: " + bookId);
  }


  @Test
  void libraryManagerShouldReturnFalseWhenReturningNotBorrowedBook() {
    assertFalse(libraryManager.returnBook("1", "1"));
  }

  @Test
  void libraryManagerShouldReturnFalseWhenReturningBookNotBorrowedByProvidedUser() {
    libraryManager.borrowBook("1", "2");
    assertFalse(libraryManager.returnBook("1", "1"));
  }


  @Test
  void testLibraryManagerBookReturn() {
    String userId = "1";
    String bookId = "1";
    int quantity = 1;
    when(userService.isUserActive(userId)).thenReturn(true);

    libraryManager.addBook(bookId, quantity);

    libraryManager.borrowBook(bookId, userId);

    boolean returnStatus = libraryManager.returnBook(bookId, userId);

    assertTrue(returnStatus);
    assertEquals(libraryManager.getAvailableCopies(bookId),quantity);
    Mockito.verify(notificationService).notifyUser(userId, "You have returned the book: " + bookId);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, -10})
  void libraryManagerShouldThrowExceptionWhenCalculatingFeeForNegativeNumberOfDays(int overdueDays) {
    var exception = assertThrows(
            IllegalArgumentException.class,
            () -> libraryManager.calculateDynamicLateFee(overdueDays, false, false)
    );
    assertEquals("Overdue days cannot be negative.", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({
          "1, false, false, 0.5",
          "1, false, true, 0.4",
          "1, true, false, 0.75",
          "1, true, true, 0.6",
  })
  void libraryManagerCalculateFee(int overdueDays, boolean isBestseller, boolean isPremiumMember, double actualAnswer) {
    assertEquals(libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember), actualAnswer);
  }

}