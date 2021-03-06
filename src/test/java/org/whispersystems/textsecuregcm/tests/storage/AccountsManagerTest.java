package org.whispersystems.textsecuregcm.tests.storage;

import org.junit.Test;
import org.whispersystems.textsecuregcm.redis.ReplicatedJedisPool;
import org.whispersystems.textsecuregcm.storage.Account;
import org.whispersystems.textsecuregcm.storage.Accounts;
import org.whispersystems.textsecuregcm.storage.AccountsManager;
import org.whispersystems.textsecuregcm.storage.DirectoryManager;

import java.util.HashSet;
import java.util.Optional;

import static junit.framework.TestCase.assertSame;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

public class AccountsManagerTest {

  @Test
  public void testGetAccountInCache() {
    ReplicatedJedisPool cacheClient      = mock(ReplicatedJedisPool.class);
    Jedis               jedis            = mock(Jedis.class              );
    Accounts            accounts         = mock(Accounts.class           );
    DirectoryManager    directoryManager = mock(DirectoryManager.class   );

    when(cacheClient.getReadResource()).thenReturn(jedis);
    when(jedis.get(eq("Account5+14152222222"))).thenReturn("{\"number\": \"+14152222222\", \"name\": \"test\"}");

    AccountsManager   accountsManager = new AccountsManager(accounts, directoryManager, cacheClient);
    Optional<Account> account         = accountsManager.get("+14152222222");

    assertTrue(account.isPresent());
    assertEquals(account.get().getNumber(), "+14152222222");
    assertEquals(account.get().getProfileName(), "test");

    verify(jedis, times(1)).get(eq("Account5+14152222222"));
    verify(jedis, times(1)).close();
    verifyNoMoreInteractions(jedis);
    verifyNoMoreInteractions(accounts);
  }

  @Test
  public void testGetAccountNotInCache() {
    ReplicatedJedisPool cacheClient      = mock(ReplicatedJedisPool.class);
    Jedis               jedis            = mock(Jedis.class              );
    Accounts            accounts         = mock(Accounts.class           );
    DirectoryManager    directoryManager = mock(DirectoryManager.class   );
    Account             account          = new Account("+14152222222", new HashSet<>(), new byte[16]);

    when(cacheClient.getReadResource()).thenReturn(jedis);
    when(cacheClient.getWriteResource()).thenReturn(jedis);
    when(jedis.get(eq("Account5+14152222222"))).thenReturn(null);
    when(accounts.get(eq("+14152222222"))).thenReturn(account);

    AccountsManager   accountsManager = new AccountsManager(accounts, directoryManager, cacheClient);
    Optional<Account> retrieved       = accountsManager.get("+14152222222");

    assertTrue(retrieved.isPresent());
    assertSame(retrieved.get(), account);

    verify(jedis, times(1)).get(eq("Account5+14152222222"));
    verify(jedis, times(1)).set(eq("Account5+14152222222"), anyString());
    verify(jedis, times(2)).close();
    verifyNoMoreInteractions(jedis);

    verify(accounts, times(1)).get(eq("+14152222222"));
    verifyNoMoreInteractions(accounts);
  }

  @Test
  public void testGetAccountBrokenCache() {
    ReplicatedJedisPool cacheClient      = mock(ReplicatedJedisPool.class);
    Jedis               jedis            = mock(Jedis.class              );
    Accounts            accounts         = mock(Accounts.class           );
    DirectoryManager    directoryManager = mock(DirectoryManager.class   );
    Account             account          = new Account("+14152222222", new HashSet<>(), new byte[16]);

    when(cacheClient.getReadResource()).thenReturn(jedis);
    when(cacheClient.getWriteResource()).thenReturn(jedis);
    when(jedis.get(eq("Account5+14152222222"))).thenThrow(new JedisException("Connection lost!"));
    when(accounts.get(eq("+14152222222"))).thenReturn(account);

    AccountsManager   accountsManager = new AccountsManager(accounts, directoryManager, cacheClient);
    Optional<Account> retrieved       = accountsManager.get("+14152222222");

    assertTrue(retrieved.isPresent());
    assertSame(retrieved.get(), account);

    verify(jedis, times(1)).get(eq("Account5+14152222222"));
    verify(jedis, times(1)).set(eq("Account5+14152222222"), anyString());
    verify(jedis, times(2)).close();
    verifyNoMoreInteractions(jedis);

    verify(accounts, times(1)).get(eq("+14152222222"));
    verifyNoMoreInteractions(accounts);
  }



}
