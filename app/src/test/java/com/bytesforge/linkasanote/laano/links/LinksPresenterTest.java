package com.bytesforge.linkasanote.laano.links;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

public class LinksPresenterTest {

    @Mock
    private LinksContract.View view;

    private LinksPresenter presenter;

    @Before
    public void setLinksPresenter() throws Exception {
        MockitoAnnotations.initMocks(this);

        presenter = new LinksPresenter(view);

        when(view.isActive()).thenReturn(true);
    }
}
