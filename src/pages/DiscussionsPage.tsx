import {
  ArrowRight,
  Bookmark,
  Eye,
  Hash,
  Heart,
  MessageCircle,
  MessageSquarePlus,
  Reply,
  SearchX,
  Send,
  Share2,
  ThumbsUp,
  TrendingUp,
} from "lucide-react";
import {
  useMemo,
  useState,
  type FormEvent,
} from "react";

import { StatCard } from "@/components/cards";
import {
  Avatar,
  Badge,
  Button,
  Card,
  CardContent,
  EmptyState,
  FilterBar,
  Modal,
  PageHeader,
  SearchBar,
  SectionTitle,
  useToast,
} from "@/components/common";
import { FormField, SelectField } from "@/components/forms";
import {
  campusDiscussions,
  discussionCategories,
  discussionCategoryOptions,
  discussionComments,
  discussionCourseOptions,
  discussionStats,
  discussionTrendingTags,
  popularDiscussionTags,
  topDiscussionContributors,
  type CampusDiscussion,
  type DiscussionComment,
} from "@/data/discussions";
import { cn } from "@/utils/cn";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

interface AskQuestionForm {
  category: string;
  course: string;
  description: string;
  tags: string;
  title: string;
}

type AskQuestionErrors = Partial<Record<keyof AskQuestionForm, string>>;

const initialAskQuestion: AskQuestionForm = {
  title: "",
  category: "",
  course: "",
  description: "",
  tags: "",
};

interface DiscussionFeedCardProps {
  discussion: CampusDiscussion;
  isBookmarked: boolean;
  isUpvoted: boolean;
  onBookmark: () => void;
  onOpen: () => void;
  onShare: () => void;
  onTagClick: (tag: string) => void;
  onUpvote: () => void;
}

function DiscussionFeedCard({
  discussion,
  isBookmarked,
  isUpvoted,
  onBookmark,
  onOpen,
  onShare,
  onTagClick,
  onUpvote,
}: DiscussionFeedCardProps) {
  return (
    <Card className="group overflow-hidden transition duration-200 hover:border-brand-200 hover:shadow-xl">
      <CardContent className="p-5 sm:p-6">
        <div className="flex items-start gap-3">
          <Avatar name={discussion.author} />
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
              <p className="text-sm font-semibold text-slate-900">
                {discussion.author}
              </p>
              <span className="text-xs text-slate-300">•</span>
              <p className="text-xs text-slate-400">{discussion.time}</p>
            </div>
            <p className="mt-0.5 truncate text-xs text-slate-500">
              {discussion.university} · {discussion.department}
            </p>
          </div>
          <Badge>{discussion.category}</Badge>
        </div>

        <button
          className="mt-5 block w-full text-left text-lg font-semibold leading-7 text-slate-950 transition group-hover:text-brand-700"
          onClick={onOpen}
          type="button"
        >
          {discussion.title}
        </button>
        <p className="mt-2 line-clamp-2 text-sm leading-6 text-slate-500">
          {discussion.preview}
        </p>

        <div className="mt-4 flex flex-wrap gap-2">
          {discussion.tags.map((tag) => (
            <button
              className="rounded-full bg-brand-50 px-2.5 py-1 text-xs font-semibold text-brand-700 ring-1 ring-inset ring-brand-600/10 transition hover:bg-brand-100"
              key={tag}
              onClick={() => onTagClick(tag)}
              type="button"
            >
              #{tag}
            </button>
          ))}
        </div>

        <div className="mt-5 flex flex-wrap items-center gap-2 border-t border-slate-100 pt-4">
          <Button
            onClick={onUpvote}
            size="sm"
            variant={isUpvoted ? "secondary" : "ghost"}
          >
            <ThumbsUp
              className={cn("size-3.5", isUpvoted && "fill-brand-600")}
            />
            {discussion.upvotes + (isUpvoted ? 1 : 0)}
          </Button>
          <span className="inline-flex h-8 items-center gap-1.5 px-2 text-xs font-medium text-slate-500">
            <MessageCircle className="size-3.5" />
            {discussion.comments}
          </span>
          <span className="inline-flex h-8 items-center gap-1.5 px-2 text-xs font-medium text-slate-500">
            <Eye className="size-3.5" />
            {discussion.views.toLocaleString()}
          </span>

          <div className="ml-auto flex items-center gap-1">
            <Button
              aria-label={
                isBookmarked
                  ? "Remove discussion bookmark"
                  : "Bookmark discussion"
              }
              onClick={onBookmark}
              size="icon"
              variant={isBookmarked ? "secondary" : "ghost"}
            >
              <Bookmark
                className={cn(
                  "size-4",
                  isBookmarked && "fill-brand-600 text-brand-600",
                )}
              />
            </Button>
            <Button
              aria-label="Share discussion"
              onClick={onShare}
              size="icon"
              variant="ghost"
            >
              <Share2 className="size-4" />
            </Button>
            <Button onClick={onOpen} size="sm" variant="outline">
              View discussion
              <ArrowRight className="size-3.5" />
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

export function DiscussionsPage() {
  const [feed, setFeed] = useState(campusDiscussions);
  const [searchValue, setSearchValue] = useState("");
  const [activeCategory, setActiveCategory] = useState("All");
  const [activeTag, setActiveTag] = useState("");
  const [bookmarkedIds, setBookmarkedIds] = useState<Set<string>>(
    new Set(["frontend-internship-interview"]),
  );
  const [upvotedIds, setUpvotedIds] = useState<Set<string>>(new Set());
  const [selectedDiscussion, setSelectedDiscussion] =
    useState<CampusDiscussion | null>(null);
  const [isAskOpen, setIsAskOpen] = useState(false);
  const [askForm, setAskForm] =
    useState<AskQuestionForm>(initialAskQuestion);
  const [askErrors, setAskErrors] = useState<AskQuestionErrors>({});
  const [likedCommentIds, setLikedCommentIds] = useState<Set<string>>(
    new Set(),
  );
  const [addedComments, setAddedComments] = useState<
    Record<string, DiscussionComment[]>
  >({});
  const [commentDraft, setCommentDraft] = useState("");
  const [replyTarget, setReplyTarget] = useState<string | null>(null);
  const { showToast } = useToast();

  useDocumentTitle("Discussions · CampusOne");

  const filteredDiscussions = useMemo(() => {
    const query = searchValue.trim().toLowerCase();

    return feed.filter((discussion) => {
      const searchableText = [
        discussion.title,
        discussion.preview,
        discussion.fullQuestion,
        discussion.course,
        discussion.category,
        discussion.author,
        discussion.university,
        ...discussion.tags,
      ]
        .join(" ")
        .toLowerCase();

      return (
        (!query || searchableText.includes(query)) &&
        (activeCategory === "All" ||
          discussion.category === activeCategory) &&
        (!activeTag ||
          discussion.tags.some(
            (tag) => tag.toLowerCase() === activeTag.toLowerCase(),
          ))
      );
    });
  }, [activeCategory, activeTag, feed, searchValue]);

  const trendingDiscussions = useMemo(
    () => [...feed].sort((a, b) => b.upvotes - a.upvotes).slice(0, 4),
    [feed],
  );
  const recentDiscussions = useMemo(
    () =>
      [...feed]
        .sort((a, b) => b.postedAt.localeCompare(a.postedAt))
        .slice(0, 4),
    [feed],
  );

  const currentComments = selectedDiscussion
    ? [
        ...(discussionComments[selectedDiscussion.id] ?? []),
        ...(addedComments[selectedDiscussion.id] ?? []),
      ]
    : [];

  const clearFilters = () => {
    setSearchValue("");
    setActiveCategory("All");
    setActiveTag("");
  };

  const selectTag = (tag: string) => {
    setActiveTag((current) =>
      current.toLowerCase() === tag.toLowerCase() ? "" : tag,
    );
    showToast({
      title: `#${tag}`,
      message: "The discussion feed has been filtered by this topic.",
    });
  };

  const toggleUpvote = (discussion: CampusDiscussion) => {
    const isUpvoted = upvotedIds.has(discussion.id);
    setUpvotedIds((current) => {
      const next = new Set(current);
      if (next.has(discussion.id)) next.delete(discussion.id);
      else next.add(discussion.id);
      return next;
    });
    showToast({
      title: isUpvoted ? "Upvote removed" : "Discussion upvoted",
      message: discussion.title,
      variant: isUpvoted ? "info" : "success",
    });
  };

  const toggleBookmark = (discussion: CampusDiscussion) => {
    const isBookmarked = bookmarkedIds.has(discussion.id);
    setBookmarkedIds((current) => {
      const next = new Set(current);
      if (next.has(discussion.id)) next.delete(discussion.id);
      else next.add(discussion.id);
      return next;
    });
    showToast({
      title: isBookmarked ? "Bookmark removed" : "Discussion saved",
      message: discussion.title,
      variant: isBookmarked ? "info" : "success",
    });
  };

  const shareDiscussion = async (discussion: CampusDiscussion) => {
    const shareData = {
      title: discussion.title,
      text: discussion.preview,
      url: `${window.location.origin}/discussions#${discussion.id}`,
    };

    if (navigator.share) {
      try {
        await navigator.share(shareData);
        return;
      } catch (error) {
        if (error instanceof DOMException && error.name === "AbortError") return;
      }
    }

    try {
      await navigator.clipboard.writeText(shareData.url);
      showToast({
        title: "Discussion link copied",
        message: "The link is ready to share.",
        variant: "success",
      });
    } catch {
      showToast({
        title: "Share discussion",
        message: shareData.url,
      });
    }
  };

  const openDiscussion = (discussion: CampusDiscussion) => {
    setSelectedDiscussion(discussion);
    setCommentDraft("");
    setReplyTarget(null);
  };

  const focusCommentComposer = (replyTo?: string) => {
    setReplyTarget(replyTo ?? null);
    window.setTimeout(() => {
      document.getElementById("discussion-comment-composer")?.focus();
    }, 0);
  };

  const postComment = () => {
    if (!selectedDiscussion || !commentDraft.trim()) return;

    const newComment: DiscussionComment = {
      id: `comment-${Date.now()}`,
      author: "Ali Khan",
      university: "COMSATS Islamabad",
      time: "Just now",
      likes: 0,
      body: commentDraft.trim(),
      replyingTo: replyTarget ?? undefined,
    };

    setAddedComments((current) => ({
      ...current,
      [selectedDiscussion.id]: [
        ...(current[selectedDiscussion.id] ?? []),
        newComment,
      ],
    }));
    setFeed((current) =>
      current.map((discussion) =>
        discussion.id === selectedDiscussion.id
          ? { ...discussion, comments: discussion.comments + 1 }
          : discussion,
      ),
    );
    setSelectedDiscussion((current) =>
      current ? { ...current, comments: current.comments + 1 } : current,
    );
    setCommentDraft("");
    setReplyTarget(null);
    showToast({
      title: "Reply posted",
      message: "Your comment now appears in this discussion.",
      variant: "success",
    });
  };

  const updateAskField = (field: keyof AskQuestionForm, value: string) => {
    setAskForm((current) => ({ ...current, [field]: value }));
    setAskErrors((current) => ({ ...current, [field]: undefined }));
  };

  const resetAskForm = () => {
    setAskForm(initialAskQuestion);
    setAskErrors({});
  };

  const handleAskSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextErrors: AskQuestionErrors = {};
    const tags = askForm.tags
      .split(",")
      .map((tag) => tag.trim().replace(/^#/, ""))
      .filter(Boolean);

    if (askForm.title.trim().length < 12) {
      nextErrors.title = "Write a clear title with at least 12 characters.";
    }
    if (!askForm.category) nextErrors.category = "Select a category.";
    if (!askForm.course) nextErrors.course = "Select a course.";
    if (askForm.description.trim().length < 40) {
      nextErrors.description =
        "Add enough detail for students to understand your question.";
    }
    if (tags.length === 0) nextErrors.tags = "Add at least one topic tag.";

    if (Object.keys(nextErrors).length > 0) {
      setAskErrors(nextErrors);
      return;
    }

    const newDiscussion: CampusDiscussion = {
      id: `question-${Date.now()}`,
      author: "Ali Khan",
      university: "COMSATS Islamabad",
      department: "Computer Science",
      time: "Just now",
      postedAt: new Date().toISOString(),
      title: askForm.title.trim(),
      preview: askForm.description.trim().slice(0, 170),
      fullQuestion: askForm.description.trim(),
      category: askForm.category,
      course: askForm.course,
      tags,
      upvotes: 0,
      comments: 0,
      views: 1,
    };

    setFeed((current) => [newDiscussion, ...current]);
    setIsAskOpen(false);
    resetAskForm();
    clearFilters();
    showToast({
      title: "Question posted",
      message: "Your discussion now appears at the top of the feed.",
      variant: "success",
    });
  };

  return (
    <div className="grid gap-8 pb-8">
      <PageHeader
        actions={
          <Button onClick={() => setIsAskOpen(true)}>
            <MessageSquarePlus className="size-4" />
            Ask question
          </Button>
        }
        description="Ask better questions, exchange practical advice, and learn from students across campus communities."
        eyebrow="Campus conversations"
        title="Discussions"
      />

      <SearchBar
        className="max-w-3xl"
        onSearch={setSearchValue}
        onValueChange={setSearchValue}
        placeholder="Search questions, topics, courses, or tags..."
        value={searchValue}
      />

      <section aria-labelledby="discussion-stats">
        <h2 className="sr-only" id="discussion-stats">
          Discussion statistics
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {discussionStats.map((stat) => (
            <div
              className="transition duration-200 hover:-translate-y-1 [&>div]:h-full [&>div]:transition-shadow [&>div]:hover:shadow-xl"
              key={stat.label}
            >
              <StatCard
                change={stat.change}
                icon={stat.icon}
                label={stat.label}
                value={stat.value}
              />
            </div>
          ))}
        </div>
      </section>

      <section>
        <FilterBar
          onClear={clearFilters}
          showClear={
            activeCategory !== "All" ||
            Boolean(activeTag) ||
            Boolean(searchValue.trim())
          }
        >
          <div className="flex flex-wrap gap-2">
            {discussionCategories.map((category) => (
              <button
                aria-pressed={activeCategory === category}
                className={cn(
                  "rounded-xl px-3 py-2 text-xs font-semibold transition",
                  activeCategory === category
                    ? "bg-brand-600 text-white shadow-sm"
                    : "bg-slate-50 text-slate-600 hover:bg-brand-50 hover:text-brand-700",
                )}
                key={category}
                onClick={() => setActiveCategory(category)}
                type="button"
              >
                {category}
              </button>
            ))}
          </div>
        </FilterBar>
      </section>

      <Card>
        <CardContent className="p-5">
          <div className="flex items-center gap-2">
            <span className="grid size-9 place-items-center rounded-xl bg-amber-50 text-amber-600">
              <TrendingUp className="size-4" />
            </span>
            <div>
              <h2 className="text-sm font-semibold text-slate-900">
                Trending topics
              </h2>
              <p className="text-xs text-slate-500">
                Join what students are discussing right now.
              </p>
            </div>
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            {discussionTrendingTags.map((tag) => {
              const isActive =
                activeTag.toLowerCase() === tag.toLowerCase();

              return (
                <button
                  aria-pressed={isActive}
                  className={cn(
                    "rounded-full px-3 py-1.5 text-sm font-semibold ring-1 ring-inset transition",
                    isActive
                      ? "bg-brand-600 text-white ring-brand-600"
                      : "bg-brand-50 text-brand-700 ring-brand-600/10 hover:-translate-y-0.5 hover:bg-brand-100",
                  )}
                  key={tag}
                  onClick={() => selectTag(tag)}
                  type="button"
                >
                  #{tag}
                </button>
              );
            })}
          </div>
        </CardContent>
      </Card>

      <div className="grid items-start gap-8 xl:grid-cols-[minmax(0,1fr)_20rem]">
        <section>
          <SectionTitle
            description={`${filteredDiscussions.length} ${filteredDiscussions.length === 1 ? "conversation" : "conversations"} in this view.`}
            title="Discussion feed"
          />
          {filteredDiscussions.length > 0 ? (
            <div className="mt-4 grid gap-4">
              {filteredDiscussions.map((discussion) => (
                <DiscussionFeedCard
                  discussion={discussion}
                  isBookmarked={bookmarkedIds.has(discussion.id)}
                  isUpvoted={upvotedIds.has(discussion.id)}
                  key={discussion.id}
                  onBookmark={() => toggleBookmark(discussion)}
                  onOpen={() => openDiscussion(discussion)}
                  onShare={() => void shareDiscussion(discussion)}
                  onTagClick={selectTag}
                  onUpvote={() => toggleUpvote(discussion)}
                />
              ))}
            </div>
          ) : (
            <EmptyState
              action={
                <Button onClick={clearFilters} variant="outline">
                  Clear filters
                </Button>
              }
              className="mt-4"
              description="Try another keyword, category, or topic tag to discover more campus conversations."
              icon={<SearchX className="size-6" />}
              title="No discussions found."
            />
          )}
        </section>

        <aside className="grid gap-4 xl:sticky xl:top-28">
          <Card>
            <CardContent className="p-5">
              <SectionTitle title="Trending discussions" />
              <div className="mt-4 grid gap-1">
                {trendingDiscussions.map((discussion, index) => (
                  <button
                    className="group flex gap-3 rounded-xl p-2.5 text-left transition hover:bg-slate-50"
                    key={discussion.id}
                    onClick={() => openDiscussion(discussion)}
                    type="button"
                  >
                    <span className="grid size-7 shrink-0 place-items-center rounded-lg bg-brand-50 text-xs font-bold text-brand-700">
                      {index + 1}
                    </span>
                    <span className="min-w-0">
                      <span className="line-clamp-2 text-sm font-semibold leading-5 text-slate-800 group-hover:text-brand-700">
                        {discussion.title}
                      </span>
                      <span className="mt-1 block text-xs text-slate-400">
                        {discussion.upvotes} upvotes
                      </span>
                    </span>
                  </button>
                ))}
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-5">
              <SectionTitle title="Recent questions" />
              <div className="mt-4 grid gap-3">
                {recentDiscussions.map((discussion) => (
                  <button
                    className="border-b border-slate-100 pb-3 text-left last:border-0 last:pb-0"
                    key={discussion.id}
                    onClick={() => openDiscussion(discussion)}
                    type="button"
                  >
                    <span className="line-clamp-2 text-sm font-medium leading-5 text-slate-700 transition hover:text-brand-700">
                      {discussion.title}
                    </span>
                    <span className="mt-1 flex items-center gap-2 text-[11px] text-slate-400">
                      <span>{discussion.time}</span>
                      <span>·</span>
                      <span>{discussion.comments} replies</span>
                    </span>
                  </button>
                ))}
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-5">
              <SectionTitle title="Top contributors" />
              <div className="mt-4 grid gap-3">
                {topDiscussionContributors.map((contributor, index) => (
                  <div
                    className="flex items-center gap-3"
                    key={contributor.name}
                  >
                    <span className="w-4 text-xs font-bold text-slate-400">
                      {index + 1}
                    </span>
                    <Avatar name={contributor.name} size="sm" />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-semibold text-slate-800">
                        {contributor.name}
                      </p>
                      <p className="text-[11px] text-slate-400">
                        {contributor.university}
                      </p>
                    </div>
                    <Badge variant="brand">
                      {contributor.points.toLocaleString()} XP
                    </Badge>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-5">
              <SectionTitle title="Popular tags" />
              <div className="mt-4 flex flex-wrap gap-2">
                {popularDiscussionTags.map((tag) => (
                  <button
                    className={cn(
                      "inline-flex items-center gap-1 rounded-full px-2.5 py-1.5 text-xs font-semibold transition",
                      activeTag.toLowerCase() === tag.label.toLowerCase()
                        ? "bg-brand-600 text-white"
                        : "bg-slate-100 text-slate-600 hover:bg-brand-50 hover:text-brand-700",
                    )}
                    key={tag.label}
                    onClick={() => selectTag(tag.label)}
                    type="button"
                  >
                    <Hash className="size-3" />
                    {tag.label}
                    <span className="opacity-60">{tag.count}</span>
                  </button>
                ))}
              </div>
            </CardContent>
          </Card>
        </aside>
      </div>

      <Modal
        description="Share enough context for students to give you a useful answer."
        footer={
          <>
            <Button
              onClick={() => {
                setIsAskOpen(false);
                resetAskForm();
              }}
              type="button"
              variant="ghost"
            >
              Cancel
            </Button>
            <Button form="ask-question-form" type="submit">
              <Send className="size-4" />
              Post question
            </Button>
          </>
        }
        isOpen={isAskOpen}
        onClose={() => setIsAskOpen(false)}
        size="lg"
        title="Ask a question"
      >
        <form
          className="grid gap-5"
          id="ask-question-form"
          noValidate
          onSubmit={handleAskSubmit}
        >
          <FormField
            error={askErrors.title}
            label="Question title"
            onChange={(event) => updateAskField("title", event.target.value)}
            placeholder="What do you need help understanding?"
            required
            value={askForm.title}
          />

          <div className="grid gap-5 sm:grid-cols-2">
            <SelectField
              error={askErrors.category}
              label="Category"
              onChange={(event) =>
                updateAskField("category", event.target.value)
              }
              options={discussionCategoryOptions}
              required
              value={askForm.category}
            />
            <SelectField
              error={askErrors.course}
              label="Course"
              onChange={(event) =>
                updateAskField("course", event.target.value)
              }
              options={discussionCourseOptions}
              required
              value={askForm.course}
            />
          </div>

          <div className="grid gap-1.5">
            <label
              className="text-sm font-semibold text-slate-700"
              htmlFor="question-description"
            >
              Description
              <span aria-hidden="true" className="ml-1 text-red-500">
                *
              </span>
            </label>
            <textarea
              aria-describedby={
                askErrors.description
                  ? "question-description-error"
                  : undefined
              }
              aria-invalid={Boolean(askErrors.description)}
              className={cn(
                "min-h-40 w-full resize-y rounded-xl border bg-white px-3.5 py-3 text-sm leading-6 text-slate-950 outline-none transition placeholder:text-slate-400 hover:border-slate-300 focus:ring-4",
                askErrors.description
                  ? "border-red-300 focus:border-red-400 focus:ring-red-100"
                  : "border-slate-200 focus:border-brand-400 focus:ring-brand-100",
              )}
              id="question-description"
              onChange={(event) =>
                updateAskField("description", event.target.value)
              }
              placeholder="Explain what you have tried, where you are stuck, and what a helpful answer would cover."
              value={askForm.description}
            />
            {askErrors.description ? (
              <p
                className="text-xs font-medium text-red-600"
                id="question-description-error"
              >
                {askErrors.description}
              </p>
            ) : null}
          </div>

          <FormField
            error={askErrors.tags}
            hint="Separate tags with commas."
            label="Tags"
            onChange={(event) => updateAskField("tags", event.target.value)}
            placeholder="java, oop, final-exams"
            required
            value={askForm.tags}
          />
        </form>
      </Modal>

      <Modal
        description={
          selectedDiscussion
            ? `${selectedDiscussion.category} · ${selectedDiscussion.course}`
            : undefined
        }
        isOpen={Boolean(selectedDiscussion)}
        onClose={() => setSelectedDiscussion(null)}
        size="xl"
        title="Discussion thread"
      >
        {selectedDiscussion ? (
          <article>
            <div className="flex items-start gap-3">
              <Avatar name={selectedDiscussion.author} size="lg" />
              <div className="min-w-0 flex-1">
                <p className="font-semibold text-slate-900">
                  {selectedDiscussion.author}
                </p>
                <p className="mt-0.5 text-xs text-slate-500">
                  {selectedDiscussion.university} ·{" "}
                  {selectedDiscussion.department} · {selectedDiscussion.time}
                </p>
              </div>
              <Badge>{selectedDiscussion.category}</Badge>
            </div>

            <h2 className="mt-6 text-2xl font-bold leading-8 tracking-tight text-slate-950">
              {selectedDiscussion.title}
            </h2>
            <div className="mt-4 grid gap-4 text-sm leading-7 text-slate-600">
              {selectedDiscussion.fullQuestion
                .split("\n\n")
                .map((paragraph) => (
                  <p key={paragraph}>{paragraph}</p>
                ))}
            </div>
            <div className="mt-5 flex flex-wrap gap-2">
              {selectedDiscussion.tags.map((tag) => (
                <button
                  className="rounded-full bg-brand-50 px-2.5 py-1 text-xs font-semibold text-brand-700 transition hover:bg-brand-100"
                  key={tag}
                  onClick={() => {
                    selectTag(tag);
                    setSelectedDiscussion(null);
                  }}
                  type="button"
                >
                  #{tag}
                </button>
              ))}
            </div>

            <div className="mt-6 flex flex-wrap items-center gap-2 border-y border-slate-100 py-4">
              <Button
                onClick={() => toggleUpvote(selectedDiscussion)}
                size="sm"
                variant={
                  upvotedIds.has(selectedDiscussion.id)
                    ? "secondary"
                    : "outline"
                }
              >
                <ThumbsUp
                  className={cn(
                    "size-3.5",
                    upvotedIds.has(selectedDiscussion.id) &&
                      "fill-brand-600",
                  )}
                />
                {selectedDiscussion.upvotes +
                  (upvotedIds.has(selectedDiscussion.id) ? 1 : 0)}{" "}
                upvotes
              </Button>
              <Button
                onClick={() => toggleBookmark(selectedDiscussion)}
                size="sm"
                variant={
                  bookmarkedIds.has(selectedDiscussion.id)
                    ? "secondary"
                    : "outline"
                }
              >
                <Bookmark
                  className={cn(
                    "size-3.5",
                    bookmarkedIds.has(selectedDiscussion.id) &&
                      "fill-brand-600",
                  )}
                />
                {bookmarkedIds.has(selectedDiscussion.id) ? "Saved" : "Save"}
              </Button>
              <Button
                onClick={() => void shareDiscussion(selectedDiscussion)}
                size="sm"
                variant="outline"
              >
                <Share2 className="size-3.5" />
                Share
              </Button>
              <Button
                className="sm:ml-auto"
                onClick={() => focusCommentComposer()}
                size="sm"
              >
                <Reply className="size-3.5" />
                Reply
              </Button>
            </div>

            <section className="mt-6">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <h3 className="font-semibold text-slate-950">Comments</h3>
                  <p className="mt-0.5 text-xs text-slate-500">
                    {selectedDiscussion.comments} community replies
                  </p>
                </div>
                <Badge variant="brand">
                  Showing {currentComments.length} recent
                </Badge>
              </div>

              <div className="mt-4 grid gap-3">
                {currentComments.length > 0 ? (
                  currentComments.map((comment) => {
                    const isLiked = likedCommentIds.has(comment.id);

                    return (
                      <div
                        className="rounded-2xl border border-slate-200 bg-slate-50/70 p-4"
                        key={comment.id}
                      >
                        <div className="flex items-start gap-3">
                          <Avatar name={comment.author} size="sm" />
                          <div className="min-w-0 flex-1">
                            <div className="flex flex-wrap items-center gap-2">
                              <p className="text-sm font-semibold text-slate-900">
                                {comment.author}
                              </p>
                              <span className="text-xs text-slate-400">
                                {comment.university}
                              </span>
                              <span className="ml-auto text-[11px] text-slate-400">
                                {comment.time}
                              </span>
                            </div>
                            {comment.replyingTo ? (
                              <p className="mt-2 text-xs font-medium text-brand-600">
                                Replying to {comment.replyingTo}
                              </p>
                            ) : null}
                            <p className="mt-2 text-sm leading-6 text-slate-600">
                              {comment.body}
                            </p>
                            <div className="mt-3 flex items-center gap-2">
                              <button
                                className={cn(
                                  "inline-flex items-center gap-1.5 rounded-lg px-2 py-1 text-xs font-semibold transition hover:bg-white",
                                  isLiked
                                    ? "text-rose-600"
                                    : "text-slate-500",
                                )}
                                onClick={() =>
                                  setLikedCommentIds((current) => {
                                    const next = new Set(current);
                                    if (next.has(comment.id)) {
                                      next.delete(comment.id);
                                    } else {
                                      next.add(comment.id);
                                    }
                                    return next;
                                  })
                                }
                                type="button"
                              >
                                <Heart
                                  className={cn(
                                    "size-3.5",
                                    isLiked && "fill-rose-500",
                                  )}
                                />
                                {comment.likes + (isLiked ? 1 : 0)}
                              </button>
                              <button
                                className="inline-flex items-center gap-1.5 rounded-lg px-2 py-1 text-xs font-semibold text-slate-500 transition hover:bg-white hover:text-brand-700"
                                onClick={() =>
                                  focusCommentComposer(comment.author)
                                }
                                type="button"
                              >
                                <Reply className="size-3.5" />
                                Reply
                              </button>
                            </div>
                          </div>
                        </div>
                      </div>
                    );
                  })
                ) : (
                  <div className="rounded-2xl border border-dashed border-slate-300 p-6 text-center">
                    <MessageCircle className="mx-auto size-6 text-slate-300" />
                    <p className="mt-2 text-sm font-semibold text-slate-700">
                      Be the first student to reply.
                    </p>
                  </div>
                )}
              </div>

              <div className="mt-5 rounded-2xl border border-slate-200 bg-white p-4">
                <div className="flex items-center gap-3">
                  <Avatar name="Ali Khan" size="sm" />
                  <div>
                    <p className="text-sm font-semibold text-slate-800">
                      Add to the conversation
                    </p>
                    {replyTarget ? (
                      <p className="text-xs text-brand-600">
                        Replying to {replyTarget}
                      </p>
                    ) : null}
                  </div>
                  {replyTarget ? (
                    <button
                      className="ml-auto text-xs font-semibold text-slate-400 hover:text-slate-700"
                      onClick={() => setReplyTarget(null)}
                      type="button"
                    >
                      Cancel reply
                    </button>
                  ) : null}
                </div>
                <textarea
                  className="mt-3 min-h-24 w-full resize-y rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-3 text-sm leading-6 text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-brand-400 focus:bg-white focus:ring-4 focus:ring-brand-100"
                  id="discussion-comment-composer"
                  onChange={(event) => setCommentDraft(event.target.value)}
                  placeholder="Write a thoughtful, helpful reply..."
                  value={commentDraft}
                />
                <div className="mt-3 flex justify-end">
                  <Button
                    disabled={!commentDraft.trim()}
                    onClick={postComment}
                    size="sm"
                  >
                    <Send className="size-3.5" />
                    Post reply
                  </Button>
                </div>
              </div>
            </section>
          </article>
        ) : null}
      </Modal>
    </div>
  );
}
