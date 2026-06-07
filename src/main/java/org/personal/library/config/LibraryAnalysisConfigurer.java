package org.personal.library.config;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

public class LibraryAnalysisConfigurer implements LuceneAnalysisConfigurer {

    @Override
    public void configure(LuceneAnalysisConfigurationContext context) {
        context.analyzer("english").custom()
                .tokenizer("standard")
                .tokenFilter("lowercase")
                .tokenFilter("stop")
                .tokenFilter("snowballPorter")
                        .param("language", "English")
                .tokenFilter("asciiFolding");
    }
}
