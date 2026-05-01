from datetime import timedelta

from django.contrib.auth.models import User
from django.test import TestCase
from django.urls import reverse
from django.utils import timezone

from .models import Article


class ArticlePagesTests(TestCase):
    def setUp(self):
        self.author = User.objects.create_user(
            username='author1',
            password='testpass123'
        )
        self.other_author = User.objects.create_user(username='author2')
        now = timezone.now()
        self.article = Article.objects.create(
            title='Перша стаття',
            text='Текст першої статті',
            publication_date=now,
            author=self.author
        )
        Article.objects.create(
            title='Інша стаття',
            text='Текст іншої статті',
            publication_date=now + timedelta(days=1),
            author=self.other_author
        )

    def test_article_list_page_shows_articles(self):
        response = self.client.get(reverse('article_list'))

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, 'Перша стаття')
        self.assertTemplateUsed(response, 'articles/article_list.html')

    def test_article_detail_page_shows_selected_article(self):
        response = self.client.get(reverse('article_detail', args=[self.article.pk]))

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, 'Текст першої статті')
        self.assertTemplateUsed(response, 'articles/article_detail.html')

    def test_articles_by_author_page_filters_articles(self):
        response = self.client.get(reverse('articles_by_author', args=[self.author.pk]))

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, 'Перша стаття')
        self.assertNotContains(response, 'Інша стаття')
        self.assertTemplateUsed(response, 'articles/articles_by_author.html')

    def test_article_api_returns_json_list(self):
        response = self.client.get(reverse('article_api_list'))

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response['Content-Type'], 'application/json')
        self.assertEqual(len(response.json()), 2)
        self.assertEqual(response.json()[0]['title'], 'Інша стаття')
