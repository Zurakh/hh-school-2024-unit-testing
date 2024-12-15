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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {

  @InjectMocks
  private LibraryManager libraryManager;

  @Mock
  private NotificationService notificationService;

  @Mock
  private UserService userService;

  // Отрицательные значения не проверяются, потому что некоторые реализации могут иметь защиту от этого
  // Несколько книг добавляются, чтобы убедиться, что система может работать с больше, чем одной книгой
  @Test
  void testLibraryManagerWithAddingSuccess() {
    // Добавление новой книги
    libraryManager.addBook("0", 7);
    libraryManager.addBook("1", 9);
    libraryManager.addBook("2", 11);
    libraryManager.addBook("3", 13);

    assertEquals(libraryManager.getAvailableCopies("0"), 7);
    assertEquals(libraryManager.getAvailableCopies("1"), 9);
    assertEquals(libraryManager.getAvailableCopies("2"), 11);
    assertEquals(libraryManager.getAvailableCopies("3"), 13);

    // Изменение количества копий
    libraryManager.addBook("0", 0);
    libraryManager.addBook("1", 1);
    libraryManager.addBook("2", 9);
    libraryManager.addBook("3", 6);

    assertEquals(libraryManager.getAvailableCopies("0"), 7);
    assertEquals(libraryManager.getAvailableCopies("1"), 10);
    assertEquals(libraryManager.getAvailableCopies("2"), 20);
    assertEquals(libraryManager.getAvailableCopies("3"), 19);
  }


  @Test
  void libraryManagerShouldReturnFalseIfAccountInactiveWhenBorrowing() {
    when(userService.isUserActive("0")).thenReturn(false);
    libraryManager.addBook("0", 5);

    assertFalse(libraryManager.borrowBook("0", "0"));

    assertEquals(libraryManager.getAvailableCopies("0"), 5);

    Mockito.verify(notificationService).notifyUser("0", "Your account is not active.");
  }

  @Test
  void libraryManagerShouldReturnFalseIfBookNotPresentInLibraryWhenBorrowing() {
    when(userService.isUserActive("1")).thenReturn(true);

    assertFalse(libraryManager.borrowBook("0", "1"));

    assertEquals(libraryManager.getAvailableCopies("0"), 0);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, -10})
  void libraryManagerShouldReturnFalseIfAvailableQuantityIsLessOrEqualToZero(int startQuantity) {
    when(userService.isUserActive("1")).thenReturn(true);
    libraryManager.addBook("1", startQuantity);

    assertFalse(libraryManager.borrowBook("1", "1"));
    assertEquals(libraryManager.getAvailableCopies("1"), startQuantity);
  }


  @ParameterizedTest
  @ValueSource(ints = {1, 10})
  void testLibraryManagerBorrowingSuccess(int startQuantity) {
    String userId = "1";
    String bookId = "1";
    when(userService.isUserActive(userId)).thenReturn(true);
    libraryManager.addBook(bookId, startQuantity);

    assertTrue(libraryManager.borrowBook(bookId, userId));
    assertEquals(libraryManager.getAvailableCopies(bookId), startQuantity-1);
    Mockito.verify(notificationService).notifyUser(userId, "You have borrowed the book: " + bookId);
  }


  @ParameterizedTest
  @ValueSource(ints = {1, 10})
  void libraryManagerShouldReturnFalseWhenReturningBookThatNotBorrowed(int startQuantity) {
    libraryManager.addBook("1", startQuantity);

    assertFalse(libraryManager.returnBook("1", "1"));

    assertEquals(libraryManager.getAvailableCopies("1"), startQuantity);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 10})
  void libraryManagerShouldReturnFalseWhenReturningBookNotBorrowedByProvidedUser(int startQuantity) {
    libraryManager.addBook("1", startQuantity);
    libraryManager.borrowBook("1", "1");

    assertFalse(libraryManager.returnBook("1", "2"));
    assertEquals(libraryManager.getAvailableCopies("1"), startQuantity);
  }


  @ParameterizedTest
  @ValueSource(ints = {1, 10})
  void testLibraryManagerBookReturnSuccess(int quantity) {
    String userId = "1";
    String bookId = "1";

    when(userService.isUserActive(userId)).thenReturn(true);

    libraryManager.addBook(bookId, quantity);

    libraryManager.borrowBook(bookId, userId);

    assertTrue(libraryManager.returnBook(bookId, userId));
    assertEquals(libraryManager.getAvailableCopies(bookId), quantity);
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